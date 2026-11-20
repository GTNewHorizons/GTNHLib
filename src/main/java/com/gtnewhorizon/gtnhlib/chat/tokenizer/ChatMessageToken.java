package com.gtnewhorizon.gtnhlib.chat.tokenizer;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record ChatMessageToken(int start, int end, ChatMessageTokenType tokenType, String text) {

    public String substring(int caretPosition) {
        return text.substring(0, Math.max(0, Math.min(text.length(), caretPosition - start + 1)));
    }
}
