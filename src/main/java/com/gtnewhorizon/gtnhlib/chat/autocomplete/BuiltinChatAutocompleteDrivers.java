package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.BuiltinCommandGrammars;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammarDriver;
import com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageToken;
import com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenType;
import com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenizer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;

import cpw.mods.fml.common.FMLCommonHandler;

public class BuiltinChatAutocompleteDrivers {

    /// Suggests command names. Other drivers can order themselves against this with `before:` or `after:`.
    public static final ResourceLocation COMMAND_NAME_DRIVER_ID = new ResourceLocation(
            "gtnhlib",
            "command-name-autocomplete");
    /// Suggests brigadier command arguments. Other drivers can order themselves against this with `before:` or
    /// `after:`.
    public static final ResourceLocation BRIGADIER_DRIVER_ID = new ResourceLocation(
            "gtnhlib",
            "brigadier-autocomplete");
    /// Suggests parameters for commands that use the grammar system.
    public static final ResourceLocation GRAMMAR_DRIVER_ID = new ResourceLocation("gtnhlib", "grammar");

    /// You can register your own commands to this without causing problems. If you want to drive your own command,
    /// place your driver before this one.
    public static final CommandGrammarDriver GRAMMAR_DRIVER = new CommandGrammarDriver(GRAMMAR_DRIVER_ID);

    public static void init() {
        ChatAutocompleteRegistry.registerAutocompleteDriver(new CommandNameDriver());

        BuiltinCommandGrammars.register(GRAMMAR_DRIVER);
        ChatAutocompleteRegistry.registerAutocompleteDriver(
                GRAMMAR_DRIVER,
                "after:" + COMMAND_NAME_DRIVER_ID,
                "before:" + BRIGADIER_DRIVER_ID);

        ChatAutocompleteRegistry
                .registerAutocompleteDriver(new BrigadierArgumentDriver(), "after:" + COMMAND_NAME_DRIVER_ID);
    }

    /// Tokenizes a pending command, without whitespace. Returns null when the text isn't a command.
    @Nullable
    private static List<ChatMessageToken> tokenizeCommand(String pendingText) {
        var tokens = ChatMessageTokenizer.tokenize(pendingText, ChatMessageTokenizer.STANDARD_SYNTAX);

        tokens.removeIf(t -> t.tokenType() == ChatMessageTokenType.WHITESPACE);

        if (tokens.isEmpty()) return null;
        if (tokens.get(0).tokenType() != ChatMessageTokenType.COMMAND_SLASH) return null;

        return tokens;
    }

    /// Suggests command names while the caret is on the slash or the command name.
    private static class CommandNameDriver implements ChatAutocompleteDriver {

        @Override
        public ResourceLocation getDriverID() {
            return COMMAND_NAME_DRIVER_ID;
        }

        @Override
        public boolean canDrive(ICommandSender sender, String pendingText, int caretPosition) {
            var tokens = tokenizeCommand(pendingText);

            if (tokens == null) return false;

            // the caret can sit in whitespace, which getTokenIndex reports as -1, so compare against the command's end
            return tokens.size() < 2 || caretPosition <= tokens.get(1).end();
        }

        @Override
        public AutocompleteSyncMode getSyncMode() {
            return AutocompleteSyncMode.MERGE;
        }

        @Override
        public AutocompleteSuggestions merge(AutocompleteSuggestions client, AutocompleteSuggestions server) {
            if (client != null && server == null) return client;
            if (client == null && server != null) return server;
            if (client == null && server == null) return null;

            HashSet<String> commands = new HashSet<>();

            commands.addAll(client.replacements());
            commands.addAll(server.replacements());

            List<String> sorted = new ArrayList<>(commands);
            sorted.sort(String::compareTo);

            return new AutocompleteSuggestions(server.snipStart(), server.snipEnd(), sorted, server.continuation());
        }

        @Override
        public AutocompleteSuggestions getSuggestions(ICommandSender sender, String pendingText, int caretPosition) {
            var tokens = tokenizeCommand(pendingText);

            if (tokens == null) return null;

            String commandFilter;
            int start, end;

            int tokenIndex = ChatMessageTokenizer.getTokenIndex(tokens, caretPosition);

            if (tokenIndex == 0) {
                // hovering after slash, show all commands
                ChatMessageToken slashToken = tokens.get(0);

                commandFilter = "";
                start = end = slashToken.end();
            } else if (tokenIndex == 1) {
                // hovering within/after command, show matching commands
                ChatMessageToken commandToken = tokens.get(1);

                commandFilter = commandToken.substring(caretPosition);
                start = commandToken.start();
                end = commandToken.end();
            } else {
                // not within command, show nothing
                return null;
            }

            CommandHandler handler;
            boolean isClient = FMLCommonHandler.instance().getEffectiveSide().isClient();

            if (isClient) {
                handler = ClientCommandHandler.instance;
            } else {
                handler = (CommandHandler) MinecraftServer.getServer().getCommandManager();
            }

            Stream<String> names = handler.getCommands().keySet().stream();

            if (!isClient) {
                // brigadier commands aren't in the vanilla command map (except as wrappers on Thermos)
                names = Stream.concat(
                        names,
                        BrigadierApi.getCommandDispatcher().getRoot().getChildren().stream()
                                .filter(node -> node.canUse(sender)).map(CommandNode::getName));
            }

            var candidates = names.filter(s -> s.startsWith(commandFilter)).distinct().sorted()
                    .collect(Collectors.toList());

            return new AutocompleteSuggestions(start, end, candidates, null);
        }
    }

    /// Suggests arguments for brigadier commands once the caret is past the command name.
    private static class BrigadierArgumentDriver implements ChatAutocompleteDriver {

        @Override
        public ResourceLocation getDriverID() {
            return BRIGADIER_DRIVER_ID;
        }

        @Override
        public boolean canDrive(ICommandSender sender, String pendingText, int caretPosition) {
            // the dispatcher only exists on the server, so whether this is a brigadier command is checked there
            var tokens = tokenizeCommand(pendingText);

            return tokens != null && tokens.size() >= 2 && caretPosition > tokens.get(1).end();
        }

        @Override
        public AutocompleteSyncMode getSyncMode() {
            return AutocompleteSyncMode.BEST_EFFORT;
        }

        @Override
        public AutocompleteSuggestions getSuggestions(ICommandSender sender, String pendingText, int caretPosition) {
            if (!pendingText.startsWith("/")) return null;

            // brigadier input has no leading slash, so every position is shifted by one
            String input = pendingText.substring(1);
            int cursor = Math.min(caretPosition - 1, input.length());

            if (cursor < 0) return null;

            CommandDispatcher<ICommandSender> dispatcher = BrigadierApi.getCommandDispatcher();
            ParseResults<ICommandSender> parse = dispatcher.parse(input, sender);

            // no root literal matched (or the sender can't use it), so this isn't a brigadier command
            if (parse.getContext().getNodes().isEmpty()) return null;

            Suggestions suggestions = dispatcher.getCompletionSuggestions(parse, cursor).join();
            String hint = BrigadierHints.getContinuation(parse, input, cursor, sender);

            if (suggestions.isEmpty() && hint == null) return null;

            return new AutocompleteSuggestions(
                    suggestions.getRange().getStart() + 1,
                    suggestions.getRange().getEnd() + 1,
                    suggestions.getList().stream().map(Suggestion::getText).collect(Collectors.toList()),
                    hint);
        }
    }
}
