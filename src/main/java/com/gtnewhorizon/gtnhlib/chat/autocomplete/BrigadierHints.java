package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.command.ICommandSender;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

/// Builds continuation hints for brigadier commands from the command tree, in the same format as the grammar hints:
/// `<argument>`, `[optional]`, `<message ...>` for greedy strings, and `<a|b>` for subcommands.
///
/// Brigadier has no hint priorities, so where the tree forks, the hint follows the first child that was registered.
public final class BrigadierHints {

    private BrigadierHints() {}

    /// The hint for the rest of the command, or null when there's nothing left to hint.
    ///
    /// @param input The command without its leading slash, as given to the dispatcher.
    /// @param cursor The caret position within the input.
    @Nullable
    public static String getContinuation(ParseResults<ICommandSender> parse, String input, int cursor,
            ICommandSender sender) {
        // the hint is drawn after the end of the text, so it only makes sense when the caret is in the last word
        if (input.indexOf(' ', cursor) != -1) return null;

        SuggestionContext<ICommandSender> context = parse.getContext().findSuggestionContext(cursor);
        String word = input.substring(context.startPos);

        if (word.isEmpty()) {
            return render(getUsableChildren(context.parent, sender), context.parent.getCommand() != null, sender);
        }

        CommandNode<ICommandSender> filled = getNodeFilledBy(parse, context, word, sender);

        if (filled == null || isGreedy(filled)) return null;

        String hint = render(getUsableChildren(filled, sender), filled.getCommand() != null, sender);

        return hint == null ? null : " " + hint;
    }

    /// The node that the word under the caret belongs to, or null when that's ambiguous. Brigadier has already parsed
    /// complete words, but a partly typed literal doesn't parse yet, so it's matched against the literal names.
    @Nullable
    private static CommandNode<ICommandSender> getNodeFilledBy(ParseResults<ICommandSender> parse,
            SuggestionContext<ICommandSender> context, String word, ICommandSender sender) {
        CommandNode<ICommandSender> parsed = getNodeStartingAt(parse.getContext(), context.startPos);

        if (parsed != null) return parsed;

        List<CommandNode<ICommandSender>> candidates = getUsableChildren(context.parent, sender).stream()
                .filter(node -> !(node instanceof LiteralCommandNode) || node.getName().startsWith(word))
                .collect(Collectors.toList());

        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    /// Searches the context and the contexts it redirects into, since each redirect starts a new context.
    @Nullable
    private static CommandNode<ICommandSender> getNodeStartingAt(CommandContextBuilder<ICommandSender> context,
            int start) {
        for (CommandContextBuilder<ICommandSender> current = context; current != null; current = current.getChild()) {
            for (ParsedCommandNode<ICommandSender> node : current.getNodes()) {
                if (node.getRange().getStart() == start) return node.getNode();
            }
        }

        return null;
    }

    /// Follows the first child down the tree until the command ends, a subcommand fork is reached, or a redirect
    /// (such as `execute ... run`) loops back to the root.
    ///
    /// @param optional True when the command can already run, which makes every remaining argument optional.
    @Nullable
    private static String render(List<CommandNode<ICommandSender>> options, boolean optional, ICommandSender sender) {
        List<String> parts = new ArrayList<>();

        while (!options.isEmpty()) {
            CommandNode<ICommandSender> node = options.get(0);
            List<String> subcommands = getLiteralNames(options);

            if (node instanceof LiteralCommandNode && subcommands.size() > 1) {
                parts.add(wrap(String.join("|", subcommands), optional));
                break;
            }

            parts.add(renderNode(node, optional));

            if (node.getRedirect() != null || isGreedy(node)) break;

            optional |= node.getCommand() != null;
            options = getUsableChildren(node, sender);
        }

        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static String renderNode(CommandNode<ICommandSender> node, boolean optional) {
        if (node instanceof LiteralCommandNode) return optional ? "[" + node.getName() + "]" : node.getName();

        String label = isGreedy(node) ? node.getName() + " ..." : node.getName();

        return wrap(label, optional);
    }

    private static String wrap(String label, boolean optional) {
        return optional ? "[" + label + "]" : "<" + label + ">";
    }

    private static boolean isGreedy(CommandNode<ICommandSender> node) {
        return node instanceof ArgumentCommandNode<?, ?>argument
                && argument.getType() instanceof StringArgumentType string
                && string.getType() == StringArgumentType.StringType.GREEDY_PHRASE;
    }

    private static List<String> getLiteralNames(List<CommandNode<ICommandSender>> nodes) {
        return nodes.stream().filter(node -> node instanceof LiteralCommandNode).map(CommandNode::getName)
                .collect(Collectors.toList());
    }

    /// Children in registration order, without the ones the sender doesn't have permission for.
    private static List<CommandNode<ICommandSender>> getUsableChildren(CommandNode<ICommandSender> node,
            ICommandSender sender) {
        return node.getChildren().stream().filter(child -> child.canUse(sender)).collect(Collectors.toList());
    }
}
