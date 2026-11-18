package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/// One argument slot in a compiled grammar, along with the slots that can follow it. States form a directed acyclic
/// graph, and are compared by identity: two slots with the same name in different branches are different states.
public final class GrammarState {

    public final String name;
    public final ArgumentType type;
    /// The slots that can come after this one once it's filled. Empty when the command can end here.
    public final List<GrammarState> next;
    /// The slots to continue with when this one is left out, or null when it can't be left out. Empty when leaving it
    /// out ends the command.
    @Nullable
    public final List<GrammarState> skip;
    /// Which branch the hint follows when several are possible. Has no effect on parsing.
    public final int hintPriority;
    /// True when this slot is inside an optional group, so the hint shows it in brackets. Unlike [#skip], this is also
    /// true for the slots after the first one in an optional sequence.
    public final boolean optional;
    /// True when this slot takes every remaining word, such as a chat message.
    public final boolean repeats;

    GrammarState(String name, ArgumentType type, List<GrammarState> next, @Nullable List<GrammarState> skip,
            int hintPriority, boolean optional, boolean repeats) {
        this.name = name;
        this.type = type;
        this.next = next;
        this.skip = skip;
        this.hintPriority = hintPriority;
        this.optional = optional;
        this.repeats = repeats;
    }

    /// How this slot appears in the hint: `name` for literals, `<name>` when required, `[name]` when optional, with a
    /// trailing `...` when it repeats.
    public String render() {
        String label = repeats ? name + " ..." : name;

        if (optional) return "[" + label + "]";

        return type.literal() ? label : "<" + label + ">";
    }

    @Override
    public String toString() {
        return render();
    }
}
