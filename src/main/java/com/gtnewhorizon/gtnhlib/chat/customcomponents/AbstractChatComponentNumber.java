package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.gtnewhorizon.gtnhlib.chat.ChatUtility;

public abstract class AbstractChatComponentNumber extends AbstractChatComponentCustom {

    protected Number number;

    public AbstractChatComponentNumber() {}

    public AbstractChatComponentNumber(Number number) {
        if (number == null) {
            throw new NullPointerException("number");
        }

        // Integral types (convert to Long / BigInteger)
        if (number instanceof Byte || number instanceof Short
                || number instanceof Integer
                || number instanceof AtomicInteger) {

            this.number = number.longValue();
            return;
        }

        if (number instanceof Long) {
            this.number = number;
            return;
        }

        if (number instanceof BigInteger bi) {
            this.number = bi;
            return;
        }

        // Floating-point types (convert to Double)
        if (number instanceof Float || number instanceof Double || number instanceof AtomicLong) {

            this.number = number.doubleValue();
            return;
        }

        // BigDecimal
        if (number instanceof BigDecimal bd) {
            BigDecimal stripped = bd.stripTrailingZeros();
            if (stripped.scale() <= 0) {
                this.number = stripped.toBigIntegerExact();
            } else {
                this.number = stripped.doubleValue();
            }
            return;
        }

        // Fallback
        throw new IllegalArgumentException("Unsupported number type: " + number.getClass().getName());
    }

    protected abstract String formatLong(long value);

    protected abstract String formatDouble(double value);

    protected abstract String formatBigInteger(BigInteger value);

    @Override
    public final String getUnformattedTextForChat() {
        if (number instanceof Long l) {
            return formatLong(l);
        }
        if (number instanceof Double d) {
            return formatDouble(d);
        }
        if (number instanceof BigInteger bi) {
            return formatBigInteger(bi);
        }

        return "[INVALID NUMBER: " + number + "]";
    }

    @Override
    public @NotNull JsonElement serialize() {
        final JsonObject obj = new JsonObject();
        ChatUtility.serializeNumber(obj, number);

        return obj;
    }

    @Override
    public void deserialize(@NotNull JsonElement jsonElement) {
        JsonObject obj = jsonElement.getAsJsonObject();
        this.number = ChatUtility.deserializeNumber(obj);
    }

}
