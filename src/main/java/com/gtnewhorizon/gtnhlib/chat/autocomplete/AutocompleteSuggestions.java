package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageToken;

/// An autocomplete suggestion for an in-progress chat message. This is shown above the chat bar. The snip start and end
/// ints are used to control where in the in-progress text the replacements should be inserted into. The snip start
/// controls the horizontal offset of the suggestion pop up. When the player presses tab, the text within the start and
/// end indices is removed and replaced with the given replacements.
/// @param continuation Text that is shown after [#snipEnd], in gray, to indicate the rest of a command's parameters.
@Desugar
public record AutocompleteSuggestions(int snipStart, int snipEnd, List<String> replacements,
        @Nullable String continuation) {

    public static AutocompleteSuggestions replace(ChatMessageToken token, List<String> replacements,
            @Nullable String continuation) {
        return new AutocompleteSuggestions(token.start(), token.end(), replacements, continuation);
    }

    public static AutocompleteSuggestions insertAfter(ChatMessageToken token, List<String> replacements,
            @Nullable String continuation) {
        return new AutocompleteSuggestions(token.end(), token.end(), replacements, continuation);
    }

    public static AutocompleteSuggestions continuation(@NotNull String continuation) {
        return new AutocompleteSuggestions(0, 0, Collections.emptyList(), continuation);
    }
}
