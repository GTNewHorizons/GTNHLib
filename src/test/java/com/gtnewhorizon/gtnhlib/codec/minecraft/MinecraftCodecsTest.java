package com.gtnewhorizon.gtnhlib.codec.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.codec.stream.ByteBufCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

class MinecraftCodecsTest {

    private static IChatComponent styledChat() {
        IChatComponent component = new ChatComponentText("Hello ");
        IChatComponent child = new ChatComponentText("world");
        child.getChatStyle().setBold(true).setColor(EnumChatFormatting.GOLD);
        return component.appendSibling(child);
    }

    @Test
    void chatStreamCodecSendsVanillaJson() {
        IChatComponent component = styledChat();

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

    @Test
    void chatIsSavedAsVanillaJsonStringAndReadsInlineJson() {
        IChatComponent component = styledChat();
        String json = IChatComponent.Serializer.func_150696_a(component);

        NBTBase nbt = MinecraftCodecs.CHAT_COMPONENT.encodeStart(NbtOps.INSTANCE, component)
                .getOrThrow(IllegalStateException::new);
        assertEquals(new NBTTagString(json), nbt);
        assertEquals(
                component,
                MinecraftCodecs.CHAT_COMPONENT.parse(NbtOps.INSTANCE, nbt).getOrThrow(IllegalStateException::new));

        JsonObject inline = new JsonObject();
        inline.addProperty("text", "inline");
        inline.addProperty("bold", true);
        IChatComponent decoded = MinecraftCodecs.CHAT_COMPONENT.parse(JsonOps.INSTANCE, inline)
                .getOrThrow(IllegalStateException::new);
        assertEquals("inline", decoded.getUnformattedText());
        assertTrue(decoded.getChatStyle().getBold());
    }

    private static final class Holder {

        private final String name;
        private final Optional<ItemStack> stack;

        Holder(String name, Optional<ItemStack> stack) {
            this.name = name;
            this.stack = stack;
        }
    }

    @Test
    void emptyOptionalItemStackIsEmptyMapAndWorksAsARecordField() {
        assertEquals(
                new NBTTagCompound(),
                MinecraftCodecs.OPTIONAL_ITEM_STACK.encodeStart(NbtOps.INSTANCE, Optional.empty())
                        .getOrThrow(IllegalStateException::new));

        Codec<Holder> codec = RecordCodecBuilder.create(
                instance -> instance
                        .group(
                                Codec.STRING.fieldOf("name").forGetter(holder -> holder.name),
                                MinecraftCodecs.OPTIONAL_ITEM_STACK.fieldOf("stack").forGetter(holder -> holder.stack))
                        .apply(instance, Holder::new));
        JsonElement encoded = codec.encodeStart(JsonOps.INSTANCE, new Holder("gem", Optional.empty()))
                .getOrThrow(IllegalStateException::new);
        assertEquals(new JsonObject(), encoded.getAsJsonObject().get("stack"));
        Holder holder = codec.parse(JsonOps.INSTANCE, encoded).result().orElseThrow(IllegalStateException::new);
        assertEquals("gem", holder.name);
        assertFalse(holder.stack.isPresent());
    }
}
