package com.gtnewhorizon.gtnhlib.util.numberformatting;

import java.math.BigInteger;

import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.chat.IChatComponentCustomSerializer;

@SuppressWarnings("unused")
public class ChatComponentNumber extends ChatComponentStyle implements IChatComponentCustomSerializer {

    protected final Number number;

    public ChatComponentNumber() {
        number = 0;
    }

    public ChatComponentNumber(long value) {
        this.number = value;
    }

    public ChatComponentNumber(double value) {
        this.number = value;
    }

    public ChatComponentNumber(BigInteger value) {
        this.number = value;
    }

    @Override
    public String getUnformattedTextForChat() {
        if (number instanceof Long) {
            return NumberFormatUtil.formatNumbers((long) number);
        }
        if (number instanceof Double) {
            return NumberFormatUtil.formatNumbers((double) number);
        }
        if (number instanceof BigInteger) {
            return NumberFormatUtil.formatNumbers((BigInteger) number);
        }

        // Error indicator.
        return "[INVALID NUMBER: " + number + "]";
    }

    @Override
    public IChatComponent createCopy() {

        final ChatComponentNumber copy;

        if (number instanceof Long l) {
            copy = new ChatComponentNumber(l);
        } else if (number instanceof Double d) {
            copy = new ChatComponentNumber(d);
        } else if (number instanceof BigInteger bi) {
            copy = new ChatComponentNumber(bi);
        } else {
            throw new IllegalStateException("Unsupported number type: " + number.getClass());
        }

        copy.setChatStyle(this.getChatStyle().createShallowCopy());

        for (IChatComponent sibling : this.getSiblings()) {
            copy.appendSibling(sibling.createCopy());
        }

        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ChatComponentNumber other = (ChatComponentNumber) obj;

        return number.equals(other.number) && getChatStyle().equals(other.getChatStyle())
                && getSiblings().equals(other.getSiblings());
    }

    @Override
    public JsonElement serialize(IChatComponent chatComponent) {
        if (!(chatComponent instanceof ChatComponentNumber num)) {
            throw new IllegalArgumentException("ChatComponentNumber serializer received: " + chatComponent.getClass());
        }

        JsonObject obj = new JsonObject();

        if (num.number instanceof Long l) {
            obj.addProperty("kind", "long");
            obj.addProperty("value", l);
        } else if (num.number instanceof Double d) {
            obj.addProperty("kind", "double");
            obj.addProperty("value", d);
        } else if (num.number instanceof BigInteger bi) {
            obj.addProperty("kind", "bigint");
            obj.addProperty("value", bi.toString());
        } else {
            throw new IllegalStateException("Unsupported number type: " + num.number.getClass());
        }

        return obj;
    }

    @Override
    public IChatComponent deserialize(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("ChatComponentNumber payload must be a JSON object");
        }

        JsonObject obj = jsonElement.getAsJsonObject();

        String kind = obj.getAsJsonPrimitive("kind").getAsString();

        switch (kind) {
            case "long" -> {
                return new ChatComponentNumber(obj.getAsJsonPrimitive("value").getAsLong());
            }
            case "double" -> {
                return new ChatComponentNumber(obj.getAsJsonPrimitive("value").getAsDouble());
            }
            case "bigint" -> {
                return new ChatComponentNumber(new BigInteger(obj.getAsJsonPrimitive("value").getAsString()));
            }
            default -> throw new JsonParseException("Unknown ChatComponentNumber kind: " + kind);
        }
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentNumber";
    }
}
