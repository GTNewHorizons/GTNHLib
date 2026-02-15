package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;

public abstract class AbstractChatComponentNumber extends AbstractChatComponentCustom {

    protected BigDecimal number = BigDecimal.ZERO;

    public AbstractChatComponentNumber() {}

    public AbstractChatComponentNumber(Number number) {
        this.number = NumberFormatUtil.bigDecimalConverter(number);
    }

    protected abstract String formatNumber(Number value);

    @Override
    public final String getUnformattedTextForChat() {
        return formatNumber(number);
    }

    @Override
    public @NotNull JsonElement serialize() {
        final JsonObject obj = new JsonObject();
        serializeNumber(obj, number);

        return obj;
    }

    @Override
    public void deserialize(@NotNull JsonElement jsonElement) {
        JsonObject obj = jsonElement.getAsJsonObject();
        this.number = deserializeNumber(obj);
    }

    private void serializeNumber(@NotNull JsonObject obj, @NotNull BigDecimal num) {
        obj.addProperty("number", num.stripTrailingZeros().toPlainString());
    }

    private BigDecimal deserializeNumber(@NotNull JsonObject obj) {
        return new BigDecimal(obj.get("number").getAsString());
    }
}
