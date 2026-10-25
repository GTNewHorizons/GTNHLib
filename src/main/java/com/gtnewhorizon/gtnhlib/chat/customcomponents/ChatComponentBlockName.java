package com.gtnewhorizon.gtnhlib.chat.customcomponents;

import net.minecraft.block.Block;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;

import cpw.mods.fml.common.network.ByteBufUtils;

/**
 * A chat component that displays the localized name of a block.
 */
public class ChatComponentBlockName extends AbstractChatComponentBuffer<ChatComponentBlockName> {

    public Block block = null;

    public ChatComponentBlockName() {}

    public ChatComponentBlockName(Block block) {
        this.block = block;
    }

    @Override
    public String getID() {
        return "gtnhlib:ChatComponentBlockName";
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        // Block instances are singletons; no copy needed.
        return new ChatComponentBlockName(block);
    }

    @Override
    public String getUnformattedTextForChat() {
        return block == null ? "" : block.getLocalizedName();
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeBoolean(block == null);
        if (block != null) {
            String name = Block.blockRegistry.getNameForObject(block);
            ByteBufUtils.writeUTF8String(buf, name == null ? "" : name);
        }
    }

    @Override
    public void decode(PacketBuffer buf) {
        if (buf.readBoolean()) {
            block = null;
            return;
        }
        String name = ByteBufUtils.readUTF8String(buf);
        block = name.isEmpty() ? null : Block.getBlockFromName(name);
    }
}
