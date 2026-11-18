package com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar;

import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.choice;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.hintPriority;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.literal;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.optional;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.param;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.rest;
import static com.gtnewhorizon.gtnhlib.chat.autocomplete.grammar.CommandGrammar.seq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

public class CommandGrammarTest {

    /// The hint for the text with the caret at the end.
    private static String hint(CommandGrammar grammar, String text) {
        CommandWords words = CommandWords.split(text, text.length());
        GrammarWalk walk = CommandGrammarDriver.walkFinishedArguments(grammar, words);

        return CommandGrammarDriver.getHint(walk, words.current());
    }

    private static boolean isDead(CommandGrammar grammar, String text) {
        CommandWords words = CommandWords.split(text, text.length());

        return CommandGrammarDriver.walkFinishedArguments(grammar, words).isDead();
    }

    @Test
    void teleportHintFollowsTheCoordinateBranch() {
        CommandGrammar tp = BuiltinCommandGrammars.teleportGrammar();

        assertEquals("[target] <x> <y> <z>", hint(tp, "/tp "));
        assertEquals("<x> <y> <z>", hint(tp, "/tp Steve "));
        assertEquals("<y> <z>", hint(tp, "/tp Steve 10 "));
        assertEquals("<y> <z>", hint(tp, "/tp 10 "));
        assertEquals(" <z>", hint(tp, "/tp Steve 10 ~5"));
        assertEquals("<z>", hint(tp, "/tp ~ ~-5 "));
        assertNull(hint(tp, "/tp Steve Alex "), "the destination branch has no slots left");
    }

    @Test
    void teleportRejectsTooManyArguments() {
        CommandGrammar tp = BuiltinCommandGrammars.teleportGrammar();

        assertFalse(isDead(tp, "/tp Steve 1 2 3 "));
        assertTrue(isDead(tp, "/tp Steve 1 2 3 4 "));
        assertTrue(isDead(tp, "/tp Steve Alex Bob "));
    }

    @Test
    void effectRendersNestedOptionals() {
        CommandGrammar effect = BuiltinCommandGrammars.effectGrammar();

        assertEquals("<player> <effect> [seconds] [amplifier]", hint(effect, "/effect "));
        assertEquals("<effect> [seconds] [amplifier]", hint(effect, "/effect Steve "));
        assertEquals("[seconds] [amplifier]", hint(effect, "/effect Steve speed "));
        assertEquals(" [seconds] [amplifier]", hint(effect, "/effect Steve speed"));
        assertEquals("[amplifier]", hint(effect, "/effect Steve speed 30 "));
        assertTrue(isDead(effect, "/effect Steve speed 30 1 5 "));
    }

    @Test
    void hintPriorityDoesNotAffectParsing() {
        CommandGrammar grammar = CommandGrammar.command(
                choice(
                        seq(literal("a"), param("first", ArgumentTypes.word())),
                        hintPriority(1, seq(literal("b"), param("second", ArgumentTypes.word())))));

        assertEquals("b <second>", hint(grammar, "/test "));
        assertEquals("<first>", hint(grammar, "/test a "));
        assertFalse(isDead(grammar, "/test a x "));
    }

    @Test
    void wholeOptionalSequencesAreBracketed() {
        CommandGrammar grammar = CommandGrammar.command(
                param("first", ArgumentTypes.word()),
                optional(seq(param("second", ArgumentTypes.word()), param("third", ArgumentTypes.word()))));

        assertEquals("<first> [second] [third]", hint(grammar, "/test "));
    }

    @Test
    void literalsWinOverMatchingArguments() {
        CommandGrammar effect = BuiltinCommandGrammars.effectGrammar();

        // `clear` also matches the effect pattern, but only the literal branch survives, which ends the command
        assertNull(hint(effect, "/effect Steve clear "));
        assertTrue(isDead(effect, "/effect Steve clear 30 "));
    }

    private static CommandGrammar builtin(String commandName) {
        CommandGrammarDriver driver = new CommandGrammarDriver(new ResourceLocation("gtnhlib", "test"));
        BuiltinCommandGrammars.register(driver);

        return driver.getGrammar(commandName);
    }

    @Test
    void subcommandForksListEveryOption() {
        assertEquals("<set|add>", hint(builtin("time"), "/time "));
        assertEquals("<value>", hint(builtin("time"), "/time set "));
        assertEquals("<objectives|players|teams>", hint(builtin("scoreboard"), "/scoreboard "));
        assertEquals(
                "<friendlyfire|color|seeFriendlyInvisibles>",
                hint(builtin("scoreboard"), "/scoreboard teams option red "));
        assertEquals("<on|off|list|reload|add|remove>", hint(builtin("whitelist"), "/whitelist "));
    }

    @Test
    void restSlotsTakeEveryRemainingWord() {
        CommandGrammar tell = builtin("tell");

        assertEquals("<player> <message ...>", hint(tell, "/tell "));
        assertNull(hint(tell, "/tell Steve hello "), "the message has been started, so it isn't hinted again");
        assertFalse(isDead(tell, "/tell Steve hello there friend "));
        assertNull(hint(builtin("scoreboard"), "/scoreboard teams join red Steve "));
        assertFalse(isDead(builtin("scoreboard"), "/scoreboard teams join red Steve Alex "));
    }

    @Test
    void restSlotsMustBeLast() {
        assertThrows(
                IllegalStateException.class,
                () -> CommandGrammar
                        .command(rest("message", ArgumentTypes.word()), param("after", ArgumentTypes.word())));
    }

    @Test
    void builtinHints() {
        assertEquals("[player] [item] [data]", hint(builtin("clear"), "/clear "));
        assertEquals("[amount] [data] [dataTag ...]", hint(builtin("give"), "/give Steve stone "));
        assertEquals("[player]", hint(builtin("xp"), "/xp 10L "));
        assertEquals("[player]", hint(builtin("gamemode"), "/gamemode c "));
        assertTrue(isDead(builtin("gamemode"), "/gamemode spectator "), "spectator doesn't exist in 1.7.10");
        assertNull(hint(builtin("kill"), "/kill "));
    }

    @Test
    void splitsAroundTheCaret() {
        CommandWords words = CommandWords.split("/tp  Steve 10", 7);

        assertEquals(1, words.finished().size());
        assertEquals("tp", words.finished().get(0).text());
        assertEquals("Steve", words.current().text());
        assertEquals(5, words.current().start());
        assertEquals(10, words.current().end());
        assertFalse(words.isLast());

        CommandWords between = CommandWords.split("/tp Steve ", 10);

        assertEquals("", between.current().text());
        assertEquals(10, between.current().start());
        assertTrue(between.isLast());

        assertNull(CommandWords.split("hello", 2));
    }
}
