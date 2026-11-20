package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Describes the arguments of a command, so that chat can suggest values for them and show a gray hint for the rest of
/// the command (`/give Steve <item> [amount] [data] [dataTag ...]`).
///
/// A grammar only drives autocomplete. It never runs or validates the command: the command still parses its own
/// arguments, so a grammar that's wrong only produces bad suggestions, never a broken command.
///
/// # Defining a grammar
///
/// Build grammars from the static factories in this class, usually with them statically imported along with
/// [ArgumentTypes]:
///
/// ```java
/// // /give <player> <item> [amount] [data] [dataTag ...]
/// command(
/// param("player", player()),
/// param("item", item()),
/// optional(seq(
/// param("amount", integer()),
/// optional(seq(param("data", integer()), optional(rest("dataTag", word())))))));
/// ```
///
/// The building blocks are:
/// - [#param(String, ArgumentType)]: one argument. The name is only used in the hint.
/// - [#literal(String)]: a fixed keyword, typically a subcommand.
/// - [#rest(String, ArgumentType)]: every remaining word, such as a chat message.
/// - [#seq(GrammarNode...)]: several nodes, one after another.
/// - [#choice(GrammarNode...)]: alternatives, such as subcommands or overloads.
/// - [#optional(GrammarNode)]: a node that can be left out. Nest optionals to say that a later argument needs an
/// earlier one, as `data` needs `amount` above.
/// - [#hintPriority(int, GrammarNode)]: which alternative the hint shows.
///
/// Register the grammar with a [CommandGrammarDriver] under each of the command's names and aliases. Grammars are
/// immutable, so one grammar can be registered under several names. Registration must happen during init and on
/// both sides, since the client and the server each work out suggestions from their own copy of the grammar.
///
/// # How input is matched
///
/// The text after the command name is split on spaces, and each word fills one [#param] or [#literal]. Arguments
/// can't contain spaces, apart from [#rest].
///
/// Every alternative of a [#choice] is followed at the same time, and an alternative is only dropped once a typed word
/// doesn't fit it. This is what makes overloads work: `/tp Steve` could be a target or a destination, and
/// only the next word (a player or a coordinate) decides which. A word fits an argument when it matches the
/// [ArgumentType]'s regex. When a word matches a literal exactly, the other alternatives that also match it are
/// dropped, so `clear` in `/effect <player> clear` isn't also read as an effect name.
///
/// The popup shows the suggestions of every alternative that's still possible, merged in declaration order, without
/// duplicates, and filtered by what the player has typed (ignoring case).
///
/// # How the hint is built
///
/// The hint shows the slots after the word being typed, and is only shown while the caret is in the last word:
/// - `<name>` is required, `[name]` is optional (along with everything inside an optional group), and `name ...` takes
/// every remaining word. Literals are shown as-is.
/// - Where the grammar branches, the hint follows the alternative with the highest [#hintPriority]. Ties go to the
/// alternative declared first.
/// - When several literals tie for the highest priority, they're shown together as `<set|add>` and the hint stops,
/// since what comes next depends on which one the player picks.
///
/// # Writing grammars that behave well
///
/// - **Keep patterns identical on both sides.** The client's suggestions are shown immediately and replaced when the
/// server's arrive. If the two sides match words differently, the hint jumps when the server responds. Knowledge
/// that only one side has (online players, registry contents) belongs in the suggester, never the pattern.
/// - **Make overloads distinguishable by pattern.** Alternatives are only told apart by which patterns a word matches.
/// [ArgumentTypes#player()] rejects purely numeric names for exactly this reason, so `/tp Steve 10` is read as
/// coordinates.
/// - **Use [ArgumentTypes#oneOf(String...)] for values and [#literal(String)] for subcommands.** Literals take
/// precedence over other arguments and are listed as `<a|b>` in the hint. Value sets are ordinary arguments, shown
/// as `<name>`. Literals are case-sensitive.
/// - **Mark the main alternative with [#hintPriority].** Without it, the first alternative declared is hinted, which
/// is often the less useful one (such as `clear` in `/effect`).
/// - **[#rest] must be the last slot in the command.** This is checked when the grammar is built.
/// - **Large suggestion lists are fine.** The driver caps them at a few hundred entries, and the list narrows as the
/// player types.
///
/// For writing new argument types, see [ArgumentType] and the examples in [ArgumentTypes]. For complete grammars, see
/// [BuiltinCommandGrammars].
public final class CommandGrammar {

    private final List<GrammarState> entries;

    private CommandGrammar(List<GrammarState> entries) {
        this.entries = entries;
    }

    /// Compiles the arguments that follow the command name, in order. With no arguments, the grammar accepts nothing
    /// after the name, which stops other drivers from suggesting arguments for the command.
    ///
    /// @throws IllegalStateException When a [#rest] slot isn't the last slot.
    public static CommandGrammar command(GrammarNode... arguments) {
        if (arguments.length == 0) return new CommandGrammar(Collections.emptyList());

        return new CommandGrammar(seq(arguments).compile(Collections.emptyList(), null, GrammarNode.Context.DEFAULT));
    }

    /// Starts a walk before the first argument.
    public GrammarWalk start() {
        return GrammarWalk.start(entries);
    }

    /// One argument, filled by one word.
    ///
    /// @param name How the argument is shown in the hint (`<name>`). It isn't used for matching.
    public static GrammarNode param(String name, ArgumentType type) {
        return new GrammarNode.Param(name, type, false);
    }

    /// A slot that takes every remaining word, such as a chat message. It must be the last slot in the command, which
    /// is checked when the grammar is compiled.
    public static GrammarNode rest(String name, ArgumentType type) {
        return new GrammarNode.Param(name, type, true);
    }

    /// A fixed keyword, such as a subcommand. Matches the word exactly, including its case, and takes precedence over
    /// other arguments that the word also matches.
    public static GrammarNode literal(String word) {
        return new GrammarNode.Param(word, ArgumentTypes.literal(word), false);
    }

    /// Nodes that come one after another.
    ///
    /// @throws IllegalArgumentException When there are no parts.
    public static GrammarNode seq(GrammarNode... parts) {
        if (parts.length == 0) throw new IllegalArgumentException("A sequence needs at least one part");

        return new GrammarNode.Seq(Arrays.asList(parts));
    }

    /// Alternatives, such as subcommands or overloads. Every option is followed until the typed words rule it out,
    /// and the suggestions of every remaining option are merged in declaration order.
    ///
    /// @throws IllegalArgumentException When there are no options.
    public static GrammarNode choice(GrammarNode... options) {
        if (options.length == 0) throw new IllegalArgumentException("A choice needs at least one option");

        return new GrammarNode.Choice(Arrays.asList(options));
    }

    /// A node that can be left out. When it's a sequence, the whole sequence is left out together, and every slot in it
    /// is shown in brackets in the hint.
    public static GrammarNode optional(GrammarNode inner) {
        return new GrammarNode.Optional(inner);
    }

    /// Sets which branch the hint follows when several are still possible. The highest priority wins, and ties go to
    /// the branch declared first. Slots inherit the priority of the closest enclosing call, and default to 0.
    ///
    /// This only affects the hint: every branch is still parsed and suggested, in declaration order.
    public static GrammarNode hintPriority(int priority, GrammarNode inner) {
        return new GrammarNode.HintPriority(priority, inner);
    }
}
