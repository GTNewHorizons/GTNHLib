package com.gtnewhorizon.gtnhlib.chat;

import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.IChatComponent;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;

public abstract class AbstractChatComponentCustom extends ChatComponentStyle {

    public abstract @NotNull JsonElement serialize();

    public abstract void deserialize(@NotNull JsonElement jsonElement);

    public abstract String getID(); // Unique ID across all mods. Make it good!

    protected abstract AbstractChatComponentCustom copySelf();

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;

        return serialize().equals(((AbstractChatComponentCustom) obj).serialize());
    }

    @Override
    public final int hashCode() {
        int result = super.hashCode();
        result = 31 * result + serialize().hashCode();
        return result;
    }

    @Override
    public final IChatComponent createCopy() {
        AbstractChatComponentCustom copy = copySelf();

        copy.setChatStyle(getChatStyle().createShallowCopy());

        for (IChatComponent sibling : getSiblings()) {
            copy.appendSibling(sibling.createCopy());
        }

        return copy;
    }

}
