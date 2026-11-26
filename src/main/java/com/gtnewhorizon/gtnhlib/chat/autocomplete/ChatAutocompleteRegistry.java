package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import com.google.common.util.concurrent.AbstractFuture;
import com.gtnewhorizon.gtnhlib.datastructs.dependencygraph.DependencyGraph;
import com.gtnewhorizon.gtnhlib.network.AutocompleteRequestPacket;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ChatAutocompleteRegistry {

    private static final DependencyGraph<ChatAutocompleteDriver> DRIVERS = new DependencyGraph<>(
            new ChatAutocompleteDriver[0]);

    private static AutocompleteFuture clientPendingSuggestion;

    private static final AtomicInteger SEQUENCE_COUNTER = new AtomicInteger(0);

    /// Registers a driver. Drivers are polled in registration order, adjusted by the given dependencies (see
    /// [com.gtnewhorizon.gtnhlib.datastructs.dependencygraph.IDependencyGraph]). Driver IDs containing a slash must be
    /// wrapped in brackets when depended on: `after:[mymod:chat/foo]`.
    public static synchronized void registerAutocompleteDriver(ChatAutocompleteDriver driver, String... dependencies) {
        DRIVERS.addObject(toGraphName(driver.getDriverID()), driver, dependencies);
    }

    /// Brackets the ID so that slashes in it aren't read as subgraph separators.
    private static String toGraphName(ResourceLocation driverId) {
        return "[" + driverId + "]";
    }

    @Nullable
    public static synchronized ChatAutocompleteDriver getAutocompleteDriver(ResourceLocation driverId) {
        return DRIVERS.getObject(toGraphName(driverId));
    }

    @Nullable
    public static synchronized ChatAutocompleteDriver getAutocompleteDriver(ICommandSender sender, String pendingText,
            int caretPosition) {
        for (var driver : DRIVERS.sorted()) {
            if (driver.canDrive(sender, pendingText, caretPosition)) {
                return driver;
            }
        }

        return null;
    }

    public static synchronized ChatAutocompleteFuture getSuggestions(ICommandSender sender, String pendingText,
            int caretPosition) {
        if (clientPendingSuggestion != null && !clientPendingSuggestion.isDone()) {
            clientPendingSuggestion.cancel(false);
            clientPendingSuggestion = null;
        }

        var driver = getAutocompleteDriver(sender, pendingText, caretPosition);

        AutocompleteFuture future = new AutocompleteFuture(
                driver != null && driver.getSyncMode() == AutocompleteSyncMode.MERGE ? driver : null);

        future.sequence = SEQUENCE_COUNTER.getAndIncrement();

        if (driver == null || !AutocompleteConfig.enabled) {
            future.finish(null);
            return future;
        }

        try {
            switch (driver.getSyncMode()) {
                case NONE -> {
                    future.finish(driver.getSuggestions(sender, pendingText, caretPosition));
                    return future;
                }
                case BEST_EFFORT, MERGE -> {
                    future.client = driver.getSuggestions(sender, pendingText, caretPosition);
                }
                default -> {

                }
            }
        } catch (RuntimeException e) {
            log.error(
                    "Caught error while getting autocomplete suggestions on the client (player={}, driver={}, text={})",
                    sender.getCommandSenderName(),
                    driver.getDriverID(),
                    pendingText,
                    e);

            if (driver.getSyncMode() == AutocompleteSyncMode.NONE) {
                future.finish(null);
                return future;
            }
        }

        switch (driver.getSyncMode()) {
            case FULL, BEST_EFFORT, MERGE -> {
                clientPendingSuggestion = future;
                NetworkHandler.instance.sendToServer(
                        new AutocompleteRequestPacket(
                                future.sequence,
                                driver.getDriverID(),
                                pendingText,
                                caretPosition));
            }
            default -> {

            }
        }

        return future;
    }

    @Internal
    public static synchronized void handleSyncedSuggestions(int sequence, AutocompleteSuggestions suggestions) {
        if (clientPendingSuggestion != null && !clientPendingSuggestion.isDone()
                && clientPendingSuggestion.sequence == sequence) {
            clientPendingSuggestion.finish(suggestions);
            clientPendingSuggestion = null;
        }
    }

    private static final class AutocompleteFuture extends AbstractFuture<@Nullable AutocompleteSuggestions>
            implements ChatAutocompleteFuture {

        private final ChatAutocompleteDriver mergeDriver;
        public int sequence;
        public AutocompleteSuggestions client;

        public AutocompleteFuture(ChatAutocompleteDriver mergeDriver) {
            this.mergeDriver = mergeDriver;
        }

        public void finish(@Nullable AutocompleteSuggestions value) {
            if (mergeDriver != null) {
                try {
                    value = mergeDriver.merge(client, value);
                } catch (RuntimeException e) {
                    log.error(
                            "Error while merging suggestions for driver {} (client={}, server={})",
                            mergeDriver.getDriverID(),
                            client,
                            value,
                            e);
                }
            }

            super.set(value);
        }

        @Override
        public boolean hasPreliminaryResults() {
            return client != null;
        }

        @Override
        public @Nullable AutocompleteSuggestions getPreliminaryResults() {
            return client;
        }
    }
}
