package com.gtnewhorizon.gtnhlib.chat;

import java.math.BigInteger;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.fluids.FluidStack;

@SuppressWarnings("unused")
public final class ChatComponentFluid extends ChatComponentStyle implements IChatComponentCustomSerializer {

    private Number number;

    public ChatComponentFluid() {
        this.number = 0L;
    }

    public ChatComponentFluid(FluidStack fluidStack) {
        this(fluidStack.amount);
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

        copy.setChatStyle(getChatStyle().createShallowCopy());

        for (final IChatComponent sibling : getSiblings()) {
            copy.appendSibling(sibling.createCopy());
        }

        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final ChatComponentFluid other = (ChatComponentFluid) obj;

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
        return "gtnhlib:ChatComponentFluid";
    }
}
