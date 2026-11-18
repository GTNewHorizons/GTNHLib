package com.gtnewhorizon.gtnhlib.chat.autocomplete;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.command.ICommandSender;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class BrigadierHintsTest {

    private static final Command<ICommandSender> RUN = context -> 1;
    private static final CommandDispatcher<ICommandSender> DISPATCHER = new CommandDispatcher<>();

    @BeforeAll
    static void registerCommands() {
        DISPATCHER.register(
                LiteralArgumentBuilder.<ICommandSender>literal("give").then(
                        RequiredArgumentBuilder.<ICommandSender, String>argument("player", word()).then(
                                RequiredArgumentBuilder.<ICommandSender, String>argument("item", word()).executes(RUN)
                                        .then(
                                                RequiredArgumentBuilder
                                                        .<ICommandSender, Integer>argument("count", integer())
                                                        .executes(RUN)))));

        DISPATCHER.register(
                LiteralArgumentBuilder.<ICommandSender>literal("time")
                        .then(
                                LiteralArgumentBuilder.<ICommandSender>literal("set").then(
                                        RequiredArgumentBuilder.<ICommandSender, Integer>argument("value", integer())
                                                .executes(RUN)))
                        .then(
                                LiteralArgumentBuilder.<ICommandSender>literal("add").then(
                                        RequiredArgumentBuilder.<ICommandSender, Integer>argument("amount", integer())
                                                .executes(RUN))));

        DISPATCHER.register(
                LiteralArgumentBuilder.<ICommandSender>literal("say").then(
                        RequiredArgumentBuilder.<ICommandSender, String>argument("message", greedyString())
                                .executes(RUN)));

        DISPATCHER.register(
                LiteralArgumentBuilder.<ICommandSender>literal("secret").requires(sender -> false).executes(RUN));

        DISPATCHER.register(
                LiteralArgumentBuilder.<ICommandSender>literal("run")
                        .then(LiteralArgumentBuilder.<ICommandSender>literal("visible").executes(RUN)).then(
                                LiteralArgumentBuilder.<ICommandSender>literal("hidden").requires(sender -> false)
                                        .executes(RUN)));

        DISPATCHER.register(
                LiteralArgumentBuilder.<ICommandSender>literal("execute")
                        .then(LiteralArgumentBuilder.<ICommandSender>literal("then").redirect(DISPATCHER.getRoot())));
    }

    /// The hint with the caret at the end of the input, which has no leading slash.
    private static String hint(String input) {
        return BrigadierHints.getContinuation(DISPATCHER.parse(input, null), input, input.length(), null);
    }

    @Test
    void argumentsAfterAnExecutableNodeAreOptional() {
        assertEquals("<player> <item> [count]", hint("give "));
        assertEquals("<item> [count]", hint("give Steve "));
        assertEquals(" <item> [count]", hint("give Steve"));
        assertEquals("[count]", hint("give Steve stone "));
        assertNull(hint("give Steve stone 5 "));
    }

    @Test
    void subcommandsAreListed() {
        assertEquals("<set|add>", hint("time "));
        assertEquals("<value>", hint("time set "));
        assertEquals(" <value>", hint("time se"), "a partly typed literal is matched by name");
    }

    @Test
    void greedyStringsEndTheHint() {
        assertEquals("<message ...>", hint("say "));
        assertNull(hint("say hello"));
        assertNull(hint("say hello there "));
    }

    @Test
    void childrenTheSenderCantUseAreHidden() {
        assertEquals("visible", hint("run "));
    }

    @Test
    void redirectsEndTheHint() {
        assertEquals("then", hint("execute "));
    }

    @Test
    void caretBeforeTheLastWordHasNoHint() {
        String input = "give Steve stone";

        assertNull(BrigadierHints.getContinuation(DISPATCHER.parse(input, null), input, 3, null));
    }
}
