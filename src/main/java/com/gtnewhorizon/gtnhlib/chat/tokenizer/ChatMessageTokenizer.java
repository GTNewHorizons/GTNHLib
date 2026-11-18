package com.gtnewhorizon.gtnhlib.chat.tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unimi.dsi.fastutil.chars.CharPredicate;

public class ChatMessageTokenizer {

    public static final CharPredicate STANDARD_SYNTAX = c -> c == '-' || c == '='
            || c == '+'
            || c == '/'
            || c == '*'
            || c == '&';

    public static List<ChatMessageToken> tokenize(String message, CharPredicate syntax) {
        if (message.isEmpty()) return Collections.emptyList();

        ChatMessageTokenType currentToken = null;
        int currentTokenStart = 0;
        boolean inString = false;

        List<ChatMessageToken> tokens = new ArrayList<>();

        int len = message.length();

        for (int i = 0; i < len; i++) {
            char c = message.charAt(i);

            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }

                continue;
            }

            char prevChar = i > 0 ? message.charAt(i - 1) : 0;
            char nextChar = i + 1 < len ? message.charAt(i + 1) : 0;
            ChatMessageTokenType tok = classify(currentToken, prevChar, c, nextChar, syntax);

            if (tok != currentToken || tok == ChatMessageTokenType.STRING) {
                if (currentToken != null) {
                    tokens.add(
                            new ChatMessageToken(
                                    currentTokenStart,
                                    i,
                                    currentToken,
                                    message.substring(currentTokenStart, i)));
                }

                currentToken = tok;
                currentTokenStart = i;
            }

            if (tok == ChatMessageTokenType.STRING) {
                inString = true;
            }
        }

        if (currentTokenStart < len) {
            tokens.add(
                    new ChatMessageToken(
                            currentTokenStart,
                            len,
                            currentToken,
                            message.substring(currentTokenStart, len)));
        }

        return tokens;
    }

    private static ChatMessageTokenType classify(ChatMessageTokenType prev, char prevChar, char currChar, char nextChar,
            CharPredicate syntax) {
        if (currChar == '"') {
            return ChatMessageTokenType.STRING;
        }

        if (Character.isWhitespace(currChar)) {
            return ChatMessageTokenType.WHITESPACE;
        }

        if (Character.isDigit(currChar)) {
            return ChatMessageTokenType.NUMBER;
        }

        if (currChar == '-' && Character.isDigit(nextChar)
                && (prev == null || prev == ChatMessageTokenType.WHITESPACE || prev == ChatMessageTokenType.SYNTAX)) {
            return ChatMessageTokenType.NUMBER;
        }

        if (prev == ChatMessageTokenType.NUMBER) {
            if (currChar == '.' || currChar == '_' || currChar == 'e' || currChar == 'E') {
                return ChatMessageTokenType.NUMBER;
            }

            if (currChar == '-' && (prevChar == 'e' || prevChar == 'E')) {
                return ChatMessageTokenType.NUMBER;
            }
        }

        if (prev == null && currChar == '/') {
            return ChatMessageTokenType.COMMAND_SLASH;
        }

        if (syntax.test(currChar)) {
            return ChatMessageTokenType.SYNTAX;
        }

        return ChatMessageTokenType.WORD;
    }

    public static int getTokenIndex(List<ChatMessageToken> tokens, int caretPosition) {
        for (int i = 0; i < tokens.size(); i++) {
            var t = tokens.get(i);

            if (caretPosition > t.start() && caretPosition <= t.end()) {
                return i;
            }
        }

        return -1;
    }
}
