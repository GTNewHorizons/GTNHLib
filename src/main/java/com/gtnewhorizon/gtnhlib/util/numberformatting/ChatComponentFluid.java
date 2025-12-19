package com.gtnewhorizon.gtnhlib.util.numberformatting;

import java.math.BigInteger;

import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.chat.IChatComponentCustomSerializer;

@SuppressWarnings("unused")
public final class ChatComponentFluid extends ChatComponentStyle implements IChatComponentCustomSerializer {

    private final Number number;

    public ChatComponentFluid() {
        this.number = 0L;
    }

    public ChatComponentFluid(long value) {
        this.number = value;
    }

    public ChatComponentFluid(double value) {
        this.number = value;
    }

    public ChatComponentFluid(BigInteger value) {
        this.number = value;
    }

    @Override
    public String getUnformattedTextForChat() {
        if (number instanceof Long) {
            return NumberFormatUtil.formatFluid((long) number);
        }
        if (number instanceof Double) {
            return NumberFormatUtil.formatFluid((double) number);
        }
        if (number instanceof BigInteger) {
            return NumberFormatUtil.formatFluid((BigInteger) number);
        }

        return "[INVALID NUMBER: " + number + "]";
    }

    @Override
    public IChatComponent createCopy() {

        final ChatComponentFluid copy;

        if (number instanceof Long l) {
            copy = new ChatComponentFluid(l);
        } else if (number instanceof Double d) {
            copy = new ChatComponentFluid(d);
        } else if (number instanceof BigInteger bi) {
            copy = new ChatComponentFluid(bi);
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

        ChatComponentFluid other = (ChatComponentFluid) obj;

        return number.equals(other.number) && getChatStyle().equals(other.getChatStyle())
                && getSiblings().equals(other.getSiblings());
    }

    @Override
    public JsonElement serialize(IChatComponent chatComponent) {
        if (!(chatComponent instanceof ChatComponentFluid fluid)) {
            throw new IllegalArgumentException("ChatComponentFluid serializer received: " + chatComponent.getClass());
        }

        JsonObject obj = new JsonObject();

        if (fluid.number instanceof Long l) {
            obj.addProperty("kind", "long");
            obj.addProperty("value", l);
        } else if (fluid.number instanceof Double d) {
            obj.addProperty("kind", "double");
            obj.addProperty("value", d);
        } else if (fluid.number instanceof BigInteger bi) {
            obj.addProperty("kind", "bigint");
            obj.addProperty("value", bi.toString());
        } else {
            throw new IllegalStateException("Unsupported number type: " + fluid.number.getClass());
        }

        return obj;
    }

    @Override
    public IChatComponent deserialize(JsonElement jsonElement) {
        if (!jsonElement.isJsonObject()) {
            throw new JsonParseException("ChatComponentFluid payload must be a JSON object");
        }

        JsonObject obj = jsonElement.getAsJsonObject();

        String kind = obj.getAsJsonPrimitive("kind").getAsString();

        switch (kind) {
            case "long" -> {
                return new ChatComponentFluid(obj.getAsJsonPrimitive("value").getAsLong());
            }
            case "double" -> {
                return new ChatComponentFluid(obj.getAsJsonPrimitive("value").getAsDouble());
            }
            case "bigint" -> {
                return new ChatComponentFluid(new BigInteger(obj.getAsJsonPrimitive("value").getAsString()));
            }
            default -> throw new JsonParseException("Unknown ChatComponentFluid kind: " + kind);
        }
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentFluid";
    }
}
