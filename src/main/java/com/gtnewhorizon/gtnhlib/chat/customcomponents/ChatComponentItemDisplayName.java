package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;

import cpw.mods.fml.common.network.ByteBufUtils;

/**
 * Resolves an item's display name on the client, without the brackets {@link ChatComponentItemName} adds. Useful
 * outside of chat, e.g. in a GUI label built on a dedicated server, where client language files are not loaded.
 */
public class ChatComponentItemDisplayName extends AbstractChatComponentBuffer<ChatComponentItemDisplayName> {

    public ItemStack stack = null;
    /**
     * If true and the display name ends with a closing parenthesis, only the contents of the last pair of parentheses
     * are kept, e.g. "Extruder Shape (Rod)" becomes "Rod". Otherwise the whole name is used.
     */
    public boolean lastParenthesesOnly = false;

    public ChatComponentItemDisplayName() {}

    public ChatComponentItemDisplayName(ItemStack stack) {
        this(stack, false);
    }

    public ChatComponentItemDisplayName(ItemStack stack, boolean lastParenthesesOnly) {
        this.stack = stack;
        this.lastParenthesesOnly = lastParenthesesOnly;
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentItemDisplayName";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentItemDisplayName(stack == null ? null : stack.copy(), lastParenthesesOnly);
    }

    @Override
    public String getUnformattedTextForChat() {
        if (stack == null) return "";
        final String name = stack.getDisplayName();
        if (name == null || name.isEmpty()) return "";
        if (lastParenthesesOnly && name.endsWith(")")) {
            final int open = name.lastIndexOf('(');
            if (open >= 0) return name.substring(open + 1, name.length() - 1);
        }
        return name;
    }

    @Override
    public void encode(PacketBuffer buf) {
        ByteBufUtils.writeItemStack(buf, this.stack);
        buf.writeBoolean(this.lastParenthesesOnly);
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.stack = ByteBufUtils.readItemStack(buf);
        this.lastParenthesesOnly = buf.readBoolean();
    }
}
