package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.command.ICommandSender;

import com.github.bsideup.jabel.Desugar;

/// The syntax of one space-separated command argument, and where its suggestions come from.
///
/// The pattern must accept the same words on the client and the server. Otherwise the two sides walk the grammar
/// differently, and the hint changes when the server's suggestions arrive. Anything only one side knows (such as which
/// players are online) belongs in the suggester, never the pattern.
///
/// @param literal True when this argument is a fixed keyword. Literals take precedence over other arguments that match
/// the same word, so that `clear` in `/effect <player> clear` isn't also read as an effect name.
@Desugar
public record ArgumentType(Pattern pattern, Suggester suggester, boolean literal) {

    @FunctionalInterface
    public interface Suggester {

        /// Returns every candidate for this argument. The caller filters them against what the player has typed.
        /// Candidates may contain spaces, in which case they fill this argument and the ones after it.
        List<String> suggest(ICommandSender sender);
    }

    public boolean matches(String word) {
        return pattern.matcher(word).matches();
    }

    /// True when the partial word matches, or could match once the player types more characters.
    public boolean couldMatch(String partial) {
        Matcher matcher = pattern.matcher(partial);

        return matcher.matches() || matcher.hitEnd();
    }
}
