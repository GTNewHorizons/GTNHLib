package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

/// A piece of a command grammar before it's compiled. Build these with the factories in [CommandGrammar].
///
/// Compiling turns the tree into a graph of [GrammarState]s, built back to front: each node is compiled already knowing
/// which states come after it, so every state's successors are fixed when it's created.
public interface GrammarNode {

    /// @param followers The states that come after this node.
    /// @param skip Where to continue when this whole node is left out, or null when it can't be left out.
    /// @param context Settings inherited from the enclosing nodes.
    /// @return The states this node can start with.
    List<GrammarState> compile(List<GrammarState> followers, @Nullable List<GrammarState> skip, Context context);

    /// Settings that apply to every slot inside a node. They only affect the hint, never parsing.
    @Desugar
    record Context(int hintPriority, boolean optional) {

        public static final Context DEFAULT = new Context(0, false);

        public Context withHintPriority(int hintPriority) {
            return new Context(hintPriority, optional);
        }

        public Context asOptional() {
            return new Context(hintPriority, true);
        }
    }

    /// @param repeats True when this slot takes every remaining word. Only valid as the last slot of a command.
    @Desugar
    record Param(String name, ArgumentType type, boolean repeats) implements GrammarNode {

        @Override
        public List<GrammarState> compile(List<GrammarState> followers, @Nullable List<GrammarState> skip,
                Context context) {
            if (repeats && !followers.isEmpty()) {
                throw new IllegalStateException("Repeating slot '" + name + "' must be the last slot in the command");
            }

            return Collections.singletonList(
                    new GrammarState(name, type, followers, skip, context.hintPriority(), context.optional(), repeats));
        }
    }

    /// Assumption: there is at least one part.
    @Desugar
    record Seq(List<GrammarNode> parts) implements GrammarNode {

        @Override
        public List<GrammarState> compile(List<GrammarState> followers, @Nullable List<GrammarState> skip,
                Context context) {
            List<GrammarState> next = followers;

            for (int i = parts.size() - 1; i > 0; i--) {
                next = parts.get(i).compile(next, null, context);
            }

            // only the first part can be skipped over, since skipping it skips the whole sequence
            return parts.get(0).compile(next, skip, context);
        }
    }

    /// Assumption: there is at least one option.
    @Desugar
    record Choice(List<GrammarNode> options) implements GrammarNode {

        @Override
        public List<GrammarState> compile(List<GrammarState> followers, @Nullable List<GrammarState> skip,
                Context context) {
            List<GrammarState> starts = new ArrayList<>();

            for (GrammarNode option : options) {
                starts.addAll(option.compile(followers, skip, context));
            }

            return starts;
        }
    }

    @Desugar
    record Optional(GrammarNode inner) implements GrammarNode {

        @Override
        public List<GrammarState> compile(List<GrammarState> followers, @Nullable List<GrammarState> skip,
                Context context) {
            // when this is the start of an optional sequence, leaving out the whole sequence is also possible
            return inner.compile(followers, skip == null ? followers : union(followers, skip), context.asOptional());
        }

        private static List<GrammarState> union(List<GrammarState> a, List<GrammarState> b) {
            Set<GrammarState> union = new LinkedHashSet<>(a);
            union.addAll(b);

            return new ArrayList<>(union);
        }
    }

    @Desugar
    record HintPriority(int priority, GrammarNode inner) implements GrammarNode {

        @Override
        public List<GrammarState> compile(List<GrammarState> followers, @Nullable List<GrammarState> skip,
                Context context) {
            return inner.compile(followers, skip, context.withHintPriority(priority));
        }
    }
}
