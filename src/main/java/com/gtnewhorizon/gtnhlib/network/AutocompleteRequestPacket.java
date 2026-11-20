package com.gtnewhorizon.gtnhlib.network;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.ApiStatus.Internal;

import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompleteConfig;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.ChatAutocompleteRegistry;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AutocompleteRequestPacket implements IPacket {

    private int sequence;
    private ResourceLocation driverId;
    private String pendingText;
    private int caretPosition;

    @Internal
    public AutocompleteRequestPacket() {
        // reflected
    }

    public AutocompleteRequestPacket(int sequence, ResourceLocation driverId, String pendingText, int caretPosition) {
        this.sequence = sequence;
        this.driverId = driverId;
        this.pendingText = pendingText;
        this.caretPosition = caretPosition;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeVarIntToBuffer(sequence);
        buf.writeStringToBuffer(driverId.toString());
        buf.writeStringToBuffer(pendingText);
        buf.writeVarIntToBuffer(caretPosition);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        sequence = buf.readVarIntFromBuffer();
        driverId = new ResourceLocation(buf.readStringFromBuffer(1024));
        pendingText = buf.readStringFromBuffer(1024);
        caretPosition = buf.readVarIntFromBuffer();
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        if (!AutocompleteConfig.enabled) return new AutocompleteSuggestionsPacket(sequence, null);

        var driver = ChatAutocompleteRegistry.getAutocompleteDriver(driverId);

        if (driver == null) {
            log.error(
                    "Player sent autocomplete request for driver {}, which is not registered on the server",
                    driverId);
            return new AutocompleteSuggestionsPacket(sequence, null);
        } else {
            try {
                return new AutocompleteSuggestionsPacket(
                        sequence,
                        driver.getSuggestions(handler.playerEntity, pendingText, caretPosition));
            } catch (RuntimeException e) {
                log.error(
                        "Caught error while getting autocomplete suggestions on the server (player={}, driver={}, text={})",
                        handler.playerEntity.getGameProfile().getName(),
                        driver.getDriverID(),
                        pendingText,
                        e);
                return new AutocompleteSuggestionsPacket(sequence, null);
            }
        }
    }
}
