package com.gtnewhorizon.gtnhlib.brigadier;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandNotFoundException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.CommandUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class BrigadierApi {

    private static final int MAX_REPORTED_USAGES = 5;

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
        CommandDispatcher<ICommandSender> dispatcher = getCommandDispatcher();
        ParseResults<ICommandSender> parse = dispatcher.parse(command, sender);
        try {
            return dispatcher.execute(parse);
        } catch (CommandSyntaxException e) {
            if (e.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand()) {
                // nothing matched at all, let vanilla report it like any other unknown command
                throw new CommandNotFoundException();
            }
            reportSyntaxError(sender, parse, e);
            return 0;
        }
    }

    /**
     * Report a parse failure as what went wrong, where it went wrong and how the matched command is meant to be used.
     * Brigadier's own message is not a translation key, so it cannot be handed to a {@link CommandException}.
     *
     * @param sender the executor
     * @param parse  the parse results the failure came from
     * @param e      the failure
     */
    private static void reportSyntaxError(ICommandSender sender, ParseResults<ICommandSender> parse,
            CommandSyntaxException e) {
        sender.addChatMessage(CommandUtils.colorChatComponent(EnumChatFormatting.RED, e.getRawMessage().getString()));

        String input = e.getInput();
        int cursor = e.getCursor();
        if (input != null && cursor >= 0 && cursor < input.length()) {
            ChatComponentText accepted = CommandUtils
                    .colorChatComponent(EnumChatFormatting.GRAY, "/" + input.substring(0, cursor));
            ChatComponentText rejected = CommandUtils
                    .colorChatComponent(EnumChatFormatting.RED, input.substring(cursor));
            rejected.getChatStyle().setUnderlined(true);
            sender.addChatMessage(accepted.appendSibling(rejected));
        }

        List<ParsedCommandNode<ICommandSender>> nodes = parse.getContext().getNodes();
        if (nodes.isEmpty()) return;

        StringBuilder path = new StringBuilder("/");
        for (ParsedCommandNode<ICommandSender> node : nodes) {
            path.append(node.getNode().getUsageText()).append(' ');
        }

        String[] usages = getCommandDispatcher().getAllUsage(nodes.get(nodes.size() - 1).getNode(), sender, true);
        for (int i = 0; i < usages.length && i < MAX_REPORTED_USAGES; i++) {
            ChatComponentTranslation usage = new ChatComponentTranslation(
                    "commands.generic.usage",
                    (path + usages[i]).trim());
            usage.getChatStyle().setColor(EnumChatFormatting.GRAY);
            sender.addChatMessage(usage);
        }
        if (usages.length > MAX_REPORTED_USAGES) {
            sender.addChatMessage(
                    CommandUtils.colorChatComponent(
                            EnumChatFormatting.GRAY,
                            "... and " + (usages.length - MAX_REPORTED_USAGES) + " more"));
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
