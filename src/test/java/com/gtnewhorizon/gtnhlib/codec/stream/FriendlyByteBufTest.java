package com.gtnewhorizon.gtnhlib.codec.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.mojang.datafixers.util.Either;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

class FriendlyByteBufTest {

    private enum Color {
        RED,
        GREEN,
        BLUE
    }

    @Test
    void helpersRoundTrip() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", 7);
        UUID id = new UUID(1, 2);

        buf.writeVarInt(300).writeVarLong(-5L).writeUtf("héllo").writeUUID(id).writeEnum(Color.BLUE)
                .writeBlockPos(new BlockPos(-10, 64, 1234567))
                .writeResourceLocation(new ResourceLocation("gtnhlib:test")).writeByteArray(new byte[] { 1, 2, 3 })
                .writeVarIntArray(new int[] { 1, -1 }).writeLongArray(new long[] { 9L }).writeNbt(tag)
                .writeArray(new String[] { "a", "b" }, FriendlyByteBuf::writeUtf);
        buf.writeCollection(Arrays.asList(1, 2), FriendlyByteBuf::writeVarInt);
        buf.writeMap(Collections.singletonMap("k", 4), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
        buf.writeOptional(Optional.of("o"), FriendlyByteBuf::writeUtf);
        buf.writeNullable((String) null, FriendlyByteBuf::writeUtf);
        buf.writeEither(Either.<String, Integer>right(5), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);

        assertEquals(300, buf.readVarInt());
        assertEquals(-5L, buf.readVarLong());
        assertEquals("héllo", buf.readUtf());
        assertEquals(id, buf.readUUID());
        assertEquals(Color.BLUE, buf.readEnum(Color.class));
        BlockPos pos = buf.readBlockPos();
        assertEquals(-10, pos.getX());
        assertEquals(64, pos.getY());
        assertEquals(1234567, pos.getZ());
        assertEquals(new ResourceLocation("gtnhlib:test"), buf.readResourceLocation());
        assertArrayEquals(new byte[] { 1, 2, 3 }, buf.readByteArray());
        assertArrayEquals(new int[] { 1, -1 }, buf.readVarIntArray());
        assertArrayEquals(new long[] { 9L }, buf.readLongArray());
        assertEquals(tag, buf.readNbt());
        assertArrayEquals(new String[] { "a", "b" }, buf.readArray(String[]::new, FriendlyByteBuf::readUtf));
        assertEquals(Arrays.asList(1, 2), buf.readList(FriendlyByteBuf::readVarInt));
        assertEquals(
                Collections.singletonMap("k", 4),
                buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt));
        assertEquals(Optional.of("o"), buf.readOptional(FriendlyByteBuf::readUtf));
        assertNull(buf.readNullable(FriendlyByteBuf::readUtf));
        assertEquals(Either.right(5), buf.readEither(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt));
        assertFalse(buf.isReadable());
    }

    @Test
    void enumCodecUsesOrdinal() {
        StreamCodec<FriendlyByteBuf, Color> codec = ByteBufCodecs.enumCodec(Color.class);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, Color.GREEN);
        assertEquals(1, buf.readableBytes());
        assertEquals(Color.GREEN, codec.decode(buf));

        FriendlyByteBuf bad = new FriendlyByteBuf(Unpooled.buffer());
        bad.writeVarInt(3);
        assertThrows(DecoderException.class, () -> codec.decode(bad));
    }

    @Test
    void arrayLengthsBeyondTheDataAreRejected() {
        FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.buffer());
        bytes.writeVarInt(Integer.MAX_VALUE);
        assertThrows(DecoderException.class, () -> bytes.readByteArray(Integer.MAX_VALUE));

        FriendlyByteBuf ints = new FriendlyByteBuf(Unpooled.buffer());
        ints.writeVarInt(Integer.MAX_VALUE);
        assertThrows(DecoderException.class, () -> ints.readVarIntArray(Integer.MAX_VALUE));

        FriendlyByteBuf negative = new FriendlyByteBuf(Unpooled.buffer());
        negative.writeVarInt(-1);
        assertThrows(DecoderException.class, negative::readLongArray);

        FriendlyByteBuf negativeCount = new FriendlyByteBuf(Unpooled.buffer());
        negativeCount.writeVarInt(-1);
        assertThrows(DecoderException.class, () -> negativeCount.readList(FriendlyByteBuf::readVarInt));

        FriendlyByteBuf hugeCount = new FriendlyByteBuf(Unpooled.buffer());
        hugeCount.writeVarInt(Integer.MAX_VALUE);
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> hugeCount.readArray(String[]::new, FriendlyByteBuf::readUtf));
    }
}
