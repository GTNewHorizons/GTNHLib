package com.gtnewhorizon.gtnhlib.chat;

import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;

public class ChatUtility {

    public static void serializeNumber(@NotNull JsonObject obj, @NotNull BigDecimal num) {
        obj.addProperty("number", num.stripTrailingZeros().toPlainString());
    }

    public static BigDecimal deserializeNumber(@NotNull JsonObject obj) {
        return new BigDecimal(obj.get("number").getAsString());
    }
}
