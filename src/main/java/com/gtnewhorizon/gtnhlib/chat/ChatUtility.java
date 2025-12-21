package com.gtnewhorizon.gtnhlib.chat;

import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ChatUtility {

    public static void serializeNumber(@NotNull JsonObject obj, @NotNull Number num) {
        if (num instanceof Long l) {
            obj.addProperty("kind", "long");
            obj.addProperty("value", l);
        } else if (num instanceof Double d) {
            obj.addProperty("kind", "double");
            obj.addProperty("value", d);
        } else if (num instanceof BigInteger bi) {
            obj.addProperty("kind", "bigint");
            obj.addProperty("value", bi.toString());
        } else {
            throw new IllegalStateException("Unsupported number type: " + num.getClass());
        }
    }

    public static Number deserializeNumber(@NotNull JsonObject obj) {

        final String kind = obj.getAsJsonPrimitive("kind").getAsString();

        return switch (kind) {
            case "long" -> obj.getAsJsonPrimitive("value").getAsLong();
            case "double" -> obj.getAsJsonPrimitive("value").getAsDouble();
            case "bigint" -> obj.getAsJsonPrimitive("value").getAsBigInteger();
            default -> throw new JsonParseException("Unknown ChatComponentNumber kind: " + kind);
        };
    }
}
