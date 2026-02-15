package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;

import cpw.mods.fml.common.network.ByteBufUtils;

public class ChatComponentItemName extends AbstractChatComponentBuffer<ChatComponentItemName> {

    protected ItemStack stack = null;

    public ChatComponentItemName() {}

    public ChatComponentItemName(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentItemName";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentItemName(stack == null ? null : stack.copy());
    }

    @Override
    public String getUnformattedTextForChat() {
        return stack == null ? "" : stack.getDisplayName();
    }

    @Override
    public void encode(PacketBuffer buf) {
        ByteBufUtils.writeItemStack(buf, this.stack);
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.stack = ByteBufUtils.readItemStack(buf);
    }
}
