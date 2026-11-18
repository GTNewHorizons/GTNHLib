package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompleteConfig;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompleteSuggestions;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.AutocompleteSyncMode;
import com.gtnewhorizon.gtnhlib.chat.autocomplete.ChatAutocompleteDriver;

/// Suggests arguments and shows a hint for commands with a registered [CommandGrammar].
///
/// Runs in [AutocompleteSyncMode#BEST_EFFORT]: grammars are plain data and exist on both sides, so the client's hint
/// appears immediately and the server's result only adds what the client can't know, such as unloaded players.
public class CommandGrammarDriver implements ChatAutocompleteDriver {

    private final ResourceLocation driverId;
    private final Map<String, CommandGrammar> grammars = new HashMap<>();

    public CommandGrammarDriver(ResourceLocation driverId) {
        this.driverId = driverId;
    }

    /// Requirement: must be called during init, on both sides, with the same grammars.
    public void register(CommandGrammar grammar, String... commandNames) {
        for (String name : commandNames) {
            grammars.put(name, grammar);
        }
    }

    @Override
    public ResourceLocation getDriverID() {
        return driverId;
    }

    @Override
    public boolean canDrive(ICommandSender sender, String pendingText, int caretPosition) {
        CommandWords words = CommandWords.split(pendingText, caretPosition);

        return words != null && getGrammar(words) != null;
    }

    @Override
    public AutocompleteSyncMode getSyncMode() {
        return AutocompleteSyncMode.BEST_EFFORT;
    }

    @Nullable
    @Override
    public AutocompleteSuggestions getSuggestions(ICommandSender sender, String pendingText, int caretPosition) {
        CommandWords words = CommandWords.split(pendingText, caretPosition);

        if (words == null) return null;

        CommandGrammar grammar = getGrammar(words);

        if (grammar == null) return null;

        GrammarWalk walk = walkFinishedArguments(grammar, words);

        if (walk.isDead()) return null;

        CommandWords.Word current = words.current();

        List<String> suggestions = walk.suggest(sender, current.text());

        // registries such as items can have tens of thousands of entries, which would be sent on every keystroke
        if (suggestions.size() > AutocompleteConfig.maxSuggestions) {
            suggestions = suggestions.subList(0, AutocompleteConfig.maxSuggestions);
        }
        String hint = words.isLast() ? getHint(walk, current) : null;

        if (suggestions.isEmpty() && hint == null) return null;

        return new AutocompleteSuggestions(current.start(), current.end(), suggestions, hint);
    }

    /// The grammar for the command, or null when the caret is still in the command name or there's no grammar for it.
    @Nullable
    private CommandGrammar getGrammar(CommandWords words) {
        if (words.finished().isEmpty()) return null;

        return getGrammar(words.finished().get(0).text());
    }

    @Nullable
    public CommandGrammar getGrammar(String commandName) {
        return grammars.get(commandName);
    }

    static GrammarWalk walkFinishedArguments(CommandGrammar grammar, CommandWords words) {
        GrammarWalk walk = grammar.start();
        List<CommandWords.Word> finished = words.finished();

        for (int i = 1; i < finished.size() && !walk.isDead(); i++) {
            walk = walk.advance(finished.get(i).text());
        }

        return walk;
    }

    /// The hint for the slots after the caret. It's drawn directly after the text, so it needs a leading space when
    /// the player is still typing a word.
    @Nullable
    static String getHint(GrammarWalk walk, CommandWords.Word current) {
        if (current.text().isEmpty()) return walk.renderHint();

        String hint = walk.advancePartial(current.text()).renderHint();

        return hint == null ? null : " " + hint;
    }
}
