package com.gtnewhorizon.gtnhlib.codec.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.codec.stream.ByteBufCodecs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

class MinecraftCodecsTest {

    @Test
    void chatStreamCodecSendsVanillaJson() {
        IChatComponent component = new ChatComponentText("Hello ");
        IChatComponent child = new ChatComponentText("world");
        child.getChatStyle().setBold(true).setColor(EnumChatFormatting.GOLD);
        component.appendSibling(child);

        ByteBuf buf = Unpooled.buffer();
        MinecraftCodecs.CHAT_COMPONENT_STREAM_CODEC.encode(buf, component);
        ByteBuf copy = buf.copy();
        assertEquals(IChatComponent.Serializer.func_150696_a(component), ByteBufCodecs.STRING_UTF8.decode(copy));

        assertEquals(component, MinecraftCodecs.CHAT_COMPONENT_STREAM_CODEC.decode(buf));
        assertFalse(buf.isReadable());
    }

    @Test
    void invalidChatJsonIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.STRING_UTF8.encode(buf, "{\"text\":");
        assertThrows(DecoderException.class, () -> MinecraftCodecs.CHAT_COMPONENT_STREAM_CODEC.decode(buf));
    }
}
