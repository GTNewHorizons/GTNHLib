package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

/// A pending command split into space-separated words, relative to the caret. Runs of spaces count as one separator.
///
/// Commands are split on spaces rather than with the chat tokenizer, because the tokenizer splits words like `~-5` into
/// several tokens, while commands see them as one argument.
///
/// @param finished The words entirely before the caret's word. The first one is the command name, without the slash.
/// @param current The word the caret is in. Empty, and positioned at the caret, when the caret is between words.
/// @param isLast True when no words come after the current one.
@Desugar
public record CommandWords(List<Word> finished, Word current, boolean isLast) {

    @Desugar
    public record Word(int start, int end, String text) {}

    /// @return The split words, or null when the text isn't a command.
    @Nullable
    public static CommandWords split(String text, int caret) {
        if (!text.startsWith("/")) return null;

        List<Word> words = getWords(text);
        List<Word> finished = new ArrayList<>();

        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);

            if (word.end() < caret) {
                finished.add(word);
            } else if (word.start() <= caret) {
                return new CommandWords(finished, word, i == words.size() - 1);
            } else {
                return new CommandWords(finished, new Word(caret, caret, ""), false);
            }
        }

        return new CommandWords(finished, new Word(caret, caret, ""), true);
    }

    private static List<Word> getWords(String text) {
        List<Word> words = new ArrayList<>();
        int i = 1;

        while (i < text.length()) {
            if (text.charAt(i) == ' ') {
                i++;
                continue;
            }

            int start = i;

            while (i < text.length() && text.charAt(i) != ' ') i++;

            words.add(new Word(start, i, text.substring(start, i)));
        }

        return words;
    }
}
