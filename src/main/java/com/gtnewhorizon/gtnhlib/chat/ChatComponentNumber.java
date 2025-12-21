package com.gtnewhorizon.gtnhlib.chat;

import java.math.BigInteger;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

@SuppressWarnings("unused")
public final class ChatComponentNumber extends ChatComponentStyle implements IChatComponentCustomSerializer {

    private Number number;

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
        if (number instanceof Long l) {
            return NumberFormatUtil.formatNumber(l);
        }
        if (number instanceof Double d) {
            return NumberFormatUtil.formatNumber(d);
        }
        if (number instanceof BigInteger bi) {
            return NumberFormatUtil.formatNumber(bi);
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

        copy.setChatStyle(getChatStyle().createShallowCopy());

        for (IChatComponent sibling : getSiblings()) {
            copy.appendSibling(sibling.createCopy());
        }

        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final ChatComponentNumber other = (ChatComponentNumber) obj;

        return number.equals(other.number) && getChatStyle().equals(other.getChatStyle())
                && getSiblings().equals(other.getSiblings());
    }

    @Override
    public JsonElement serialize() {
        final JsonObject obj = new JsonObject();
        ChatUtility.serializeNumber(obj, number);

        return obj;
    }

    @Override
    public void deserialize(JsonElement jsonElement) {
        JsonObject obj = jsonElement.getAsJsonObject();
        this.number = ChatUtility.deserializeNumber(obj);
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentNumber";
    }
}
