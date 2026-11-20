package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.minecraft.command.ICommandSender;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/// The grammar slots that are still possible after some words have been typed. Every possible branch is walked at
/// once, since overloads often can't be told apart until later words are typed (`/tp a b` vs `/tp a x y z`).
///
/// Walks are immutable: advancing returns a new walk.
public final class GrammarWalk {

    /// The slots the next word can fill, before including the slots reachable by leaving out optional ones.
    private final List<GrammarState> live;
    /// Repeating slots that have already taken a word and can take more. They're kept apart from [#live] so that the
    /// hint doesn't show them again.
    private final List<GrammarState> repeated;
    /// True when some branch has filled all of its slots, meaning the command can already be complete.
    private final boolean complete;

    private GrammarWalk(List<GrammarState> live, List<GrammarState> repeated, boolean complete) {
        this.live = live;
        this.repeated = repeated;
        this.complete = complete;
    }

    static GrammarWalk start(List<GrammarState> entries) {
        return new GrammarWalk(entries, Collections.emptyList(), entries.isEmpty());
    }

    /// True when no branch accepts the words typed so far.
    public boolean isDead() {
        return live.isEmpty() && repeated.isEmpty() && !complete;
    }

    /// Every slot the next word can fill, including the ones reached by leaving out optional slots.
    public List<GrammarState> getCandidates() {
        Set<GrammarState> candidates = new LinkedHashSet<>();

        for (GrammarState state : live) {
            addWithSkips(state, candidates);
        }

        candidates.addAll(repeated);

        return new ArrayList<>(candidates);
    }

    private static void addWithSkips(GrammarState state, Set<GrammarState> candidates) {
        if (!candidates.add(state) || state.skip == null) return;

        for (GrammarState skipped : state.skip) {
            addWithSkips(skipped, candidates);
        }
    }

    /// Fills the next slot with a finished word. Literals that match the word win over other arguments.
    @Contract(pure = true)
    public GrammarWalk advance(String word) {
        List<GrammarState> matched = filterCandidates(state -> state.type.matches(word));

        if (matched.stream().anyMatch(state -> state.type.literal())) {
            matched.removeIf(state -> !state.type.literal());
        }

        return follow(matched);
    }

    /// Fills the next slot with a word that's still being typed. Literals don't win here, since the player may be
    /// typing a longer word that starts with the literal.
    @Contract(pure = true)
    public GrammarWalk advancePartial(String partial) {
        return follow(filterCandidates(state -> state.type.couldMatch(partial)));
    }

    private List<GrammarState> filterCandidates(Predicate<GrammarState> filter) {
        return getCandidates().stream().filter(filter).collect(Collectors.toList());
    }

    private static GrammarWalk follow(List<GrammarState> filled) {
        Set<GrammarState> next = new LinkedHashSet<>();
        List<GrammarState> repeated = new ArrayList<>();
        boolean complete = false;

        for (GrammarState state : filled) {
            next.addAll(state.next);
            complete |= state.next.isEmpty();

            if (state.repeats) repeated.add(state);
        }

        return new GrammarWalk(new ArrayList<>(next), repeated, complete);
    }

    /// The suggestions for the next slot, merged across every branch and filtered by the partial word. Earlier
    /// branches' suggestions come first, and duplicates are dropped.
    public List<String> suggest(ICommandSender sender, String partial) {
        String prefix = partial.toLowerCase(Locale.ROOT);
        Set<String> suggestions = new LinkedHashSet<>();

        for (GrammarState state : getCandidates()) {
            for (String suggestion : state.type.suggester().suggest(sender)) {
                if (suggestion.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(suggestion);
                }
            }
        }

        return new ArrayList<>(suggestions);
    }

    /// The hint for the rest of the command, such as `<player> <effect> [seconds] [amplifier]`. Wherever the grammar
    /// forks, the hint follows the branch with the highest [GrammarState#hintPriority]. When several subcommands tie
    /// for
    /// the highest priority, the hint lists them all (`<set|add>`) and stops there, since the rest depends on which one
    /// the player picks. Returns null when no slots are left.
    @Nullable
    public String renderHint() {
        List<String> parts = new ArrayList<>();
        List<GrammarState> options = live;

        while (true) {
            GrammarState state = getHighestPriority(options);

            if (state == null) break;

            List<String> subcommands = getLiteralNames(options, state.hintPriority);

            if (state.type.literal() && subcommands.size() > 1) {
                parts.add("<" + String.join("|", subcommands) + ">");
                break;
            }

            parts.add(state.render());
            options = state.next;
        }

        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static List<String> getLiteralNames(List<GrammarState> states, int hintPriority) {
        return states.stream().filter(state -> state.type.literal() && state.hintPriority == hintPriority)
                .map(state -> state.name).distinct().collect(Collectors.toList());
    }

    /// Ties go to the earliest state, which is the one declared first in the grammar.
    @Nullable
    private static GrammarState getHighestPriority(List<GrammarState> states) {
        GrammarState best = null;

        for (GrammarState state : states) {
            if (best == null || state.hintPriority > best.hintPriority) {
                best = state;
            }
        }

        return best;
    }

    @Override
    public String toString() {
        return "GrammarWalk{live=" + live + ", repeated=" + repeated + ", complete=" + complete + "}";
    }
}
