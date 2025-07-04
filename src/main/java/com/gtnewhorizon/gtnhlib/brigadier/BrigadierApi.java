package com.gtnewhorizon.gtnhlib.brigadier;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BrigadierApi {

    @Nullable
    private static CommandDispatcher<ICommandSender> dispatcher = new CommandDispatcher<>();

    @NotNull
    public static CommandDispatcher<ICommandSender> getCommandDispatcher() {
        return Objects.requireNonNull(dispatcher);
    }

    @ApiStatus.Internal
    public static void init() {
        dispatcher = new CommandDispatcher<>();
        registerTestCommand();
    }

    @ApiStatus.Internal
    public static void clear() {
        dispatcher = null;
    }

    private static void registerTestCommand() {
        getCommandDispatcher().register(LiteralArgumentBuilder.<ICommandSender>literal("brigadier").executes(ctx -> {
            ctx.getSource().addChatMessage(new ChatComponentText("Brigadier from GTNHLib!"));
            return Command.SINGLE_SUCCESS;
        }).then(
                RequiredArgumentBuilder
                        .<ICommandSender, String>argument("stringValue", StringArgumentType.greedyString())
                        .requires(src -> src.canCommandSenderUseCommand(4, "")).suggests((ctx, builder) -> {
                            for (EntityPlayer playerEntity : ctx.getSource().getEntityWorld().playerEntities) {
                                builder.suggest(playerEntity.getCommandSenderName());
                            }
                            for (int i = 0; i < 9; i++) {
                                builder.suggest(i);
                            }
                            return builder.buildFuture();
                        }).executes(ctx -> {
                            String value = StringArgumentType.getString(ctx, "stringValue");
                            ctx.getSource().addChatMessage(new ChatComponentText("Brigadier: " + value));
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    /**
     * Execute the command by brigadier.
     * 
     * @param sender  the executor
     * @param command the command to execute
     * @return the result
     */
    public static int executeCommand(ICommandSender sender, String command) {
        try {
            return getCommandDispatcher().execute(command, sender);
        } catch (CommandSyntaxException e) {
            // TODO: make the exception message better
            throw new CommandException(e.getMessage());
        }
    }

    /**
     * Return the suggestions for the given sender with the given typed command.
     * 
     * @param sender  the given sender
     * @param command the given typed command
     * @return the suggestions in text
     */
    public static List<String> getPossibleCommands(ICommandSender sender, String command) {
        ParseResults<ICommandSender> parse = getCommandDispatcher().parse(command, sender);
        try {
            return getCommandDispatcher().getCompletionSuggestions(parse).get().getList().stream()
                    .map(Suggestion::getText).collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Exception occurred while completing command '{}'", command, e);
            return Collections.emptyList();
        }
    }

    /**
     * Return the wrapped commands that the given sender has the permission to execute.
     * 
     * @param sender the given sender
     * @return the wrapped commands
     */
    public static Collection<ICommand> getPossibleCommands(ICommandSender sender) {
        return getCommandDispatcher().getRoot().getChildren().stream().filter(c -> c.canUse(sender))
                .map(BrigadierApi::toICommandForHelpCommand).collect(Collectors.toList());
    }

    /**
     * Wrap the given command node to {@link ICommand} with only used methods implemented.
     *
     * @param node the given command node
     * @return the wrapper
     */
    private static ICommand toICommandForHelpCommand(CommandNode<?> node) {
        return new ICommand() {

            @Override
            public String getCommandName() {
                return node.getName();
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return node.getUsageText();
            }

            @Override
            public List<String> getCommandAliases() {
                return Collections.emptyList();
            }

            @Override
            public void processCommand(ICommandSender sender, String[] args) {}

            @Override
            public boolean canCommandSenderUseCommand(ICommandSender sender) {
                return false;
            }

            @Override
            public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
                return Collections.emptyList();
            }

            @Override
            public boolean isUsernameIndex(String[] args, int index) {
                return false;
            }

            @Override
            public int compareTo(@NotNull Object o) {
                if (o instanceof ICommand otherCommand) {
                    return getCommandName().compareTo(otherCommand.getCommandName());
                }
                return 0;
            }

            @Override
            public String toString() {
                return "DummyICommand{" + node.toString() + "}";
            }

            @Override
            public int hashCode() {
                return node.hashCode();
            }
        };
    }

}
