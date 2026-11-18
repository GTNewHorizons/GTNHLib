package com.gtnewhorizon.gtnhlib.chat.tokenizer;

import static com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenType.COMMAND_SLASH;
import static com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenType.NUMBER;
import static com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenType.STRING;
import static com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenType.SYNTAX;
import static com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenType.WHITESPACE;
import static com.gtnewhorizon.gtnhlib.chat.tokenizer.ChatMessageTokenType.WORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ChatMessageTokenizerTest {

    /// Tokenizes with [ChatMessageTokenizer#STANDARD_SYNTAX], checks that the tokens cover the whole message with
    /// consistent offsets, then compares against the expected `TYPE:text` pairs.
    private static void check(String message, Object... expected) {
        List<ChatMessageToken> tokens = ChatMessageTokenizer.tokenize(message, ChatMessageTokenizer.STANDARD_SYNTAX);

        int pos = 0;
        for (ChatMessageToken token : tokens) {
            assertEquals(pos, token.start(), () -> "token gap/overlap in " + tokens);
            assertTrue(token.end() > token.start(), () -> "empty token in " + tokens);
            assertEquals(message.substring(token.start(), token.end()), token.text());
            pos = token.end();
        }
        assertEquals(message.length(), pos, () -> "tokens don't cover message: " + tokens);

        List<String> expectedStrings = new ArrayList<>();
        for (int i = 0; i < expected.length; i += 2) {
            expectedStrings.add(expected[i] + ":" + expected[i + 1]);
        }

        List<String> actualStrings = new ArrayList<>();
        for (ChatMessageToken token : tokens) {
            actualStrings.add(token.tokenType() + ":" + token.text());
        }

        assertEquals(expectedStrings, actualStrings, () -> "tokenizing: " + message);
    }

    @Test
    void testEmpty() {
        check("");
    }

    @Test
    void testSingleTokens() {
        check("hello", WORD, "hello");
        check("123", NUMBER, "123");
        check("   ", WHITESPACE, "   ");
        check("+", SYNTAX, "+");
    }

    @Test
    void testCommand() {
        check("/tp", COMMAND_SLASH, "/", WORD, "tp");
        check(
                "/tp player 1 2 3",
                COMMAND_SLASH,
                "/",
                WORD,
                "tp",
                WHITESPACE,
                " ",
                WORD,
                "player",
                WHITESPACE,
                " ",
                NUMBER,
                "1",
                WHITESPACE,
                " ",
                NUMBER,
                "2",
                WHITESPACE,
                " ",
                NUMBER,
                "3");
    }

    @Test
    void testSlashOnlyCommandAtStart() {
        check("a/b", WORD, "a", SYNTAX, "/", WORD, "b");
        check(" /tp", WHITESPACE, " ", SYNTAX, "/", WORD, "tp");
        check("//wand", COMMAND_SLASH, "/", SYNTAX, "/", WORD, "wand");
    }

    @Test
    void testWhitespaceRunsMerge() {
        check("a  \tb", WORD, "a", WHITESPACE, "  \t", WORD, "b");
    }

    @Test
    void testSyntaxRunsMerge() {
        check("a+=b", WORD, "a", SYNTAX, "+=", WORD, "b");
    }

    @Test
    void testCustomSyntax() {
        List<ChatMessageToken> tokens = ChatMessageTokenizer.tokenize("a:b+c", c -> c == ':');

        assertEquals(3, tokens.size());
        assertEquals(SYNTAX, tokens.get(1).tokenType());
        assertEquals(":", tokens.get(1).text());
        assertEquals(WORD, tokens.get(2).tokenType());
        assertEquals("b+c", tokens.get(2).text());
    }

    @Test
    void testNumbers() {
        check("1.5", NUMBER, "1.5");
        check("1_000_000", NUMBER, "1_000_000");
        check("1e5", NUMBER, "1e5");
        check("1E5", NUMBER, "1E5");
        check("1e-5", NUMBER, "1e-5");
    }

    @Test
    void testNegativeNumbers() {
        check("-123", NUMBER, "-123");
        check("-1.5", NUMBER, "-1.5");
        check("tp 5 -3", WORD, "tp", WHITESPACE, " ", NUMBER, "5", WHITESPACE, " ", NUMBER, "-3");
        check("x=-5", WORD, "x", SYNTAX, "=", NUMBER, "-5");
    }

    @Test
    void testMinusWithoutDigitIsSyntax() {
        check("-", SYNTAX, "-");
        check("- 3", SYNTAX, "-", WHITESPACE, " ", NUMBER, "3");
        check("-x", SYNTAX, "-", WORD, "x");
    }

    @Test
    void testMinusAfterNumber() {
        check("5-3", NUMBER, "5", SYNTAX, "-", NUMBER, "3");
    }

    @Test
    void testMinusAfterWord() {
        check("x-3", WORD, "x", SYNTAX, "-", NUMBER, "3");
    }

    @Test
    void testDigitsAfterWord() {
        check("abc123", WORD, "abc", NUMBER, "123");
    }

    @Test
    void testString() {
        check("\"hello world\"", STRING, "\"hello world\"");
    }

    @Test
    void testStringEndsAtClosingQuote() {
        check("say \"hi\" there", WORD, "say", WHITESPACE, " ", STRING, "\"hi\"", WHITESPACE, " ", WORD, "there");
        check("\"hi\"x", STRING, "\"hi\"", WORD, "x");
        check("\"hi\"5", STRING, "\"hi\"", NUMBER, "5");
    }

    @Test
    void testAdjacentStrings() {
        check("\"a\"\"b\"", STRING, "\"a\"", STRING, "\"b\"");
    }

    @Test
    void testEmptyString() {
        check("\"\"", STRING, "\"\"");
    }

    @Test
    void testStringContentsNotClassified() {
        check("\"/tp -5 +\"", STRING, "\"/tp -5 +\"");
    }

    @Test
    void testEscapedQuote() {
        check("\"a\\\"b\"", STRING, "\"a\\\"b\"");
        check("\"a\\\"b\" c", STRING, "\"a\\\"b\"", WHITESPACE, " ", WORD, "c");
    }

    @Test
    void testEscapedBackslash() {
        check("\"a\\\\\" c", STRING, "\"a\\\\\"", WHITESPACE, " ", WORD, "c");
    }

    @Test
    void testUnterminatedString() {
        check("say \"hello", WORD, "say", WHITESPACE, " ", STRING, "\"hello");
        check("\"abc\\", STRING, "\"abc\\");
    }

    @Test
    void testBackslashOutsideString() {
        check("a\\b", WORD, "a\\b");
    }
}
