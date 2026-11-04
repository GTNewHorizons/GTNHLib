package com.gtnewhorizon.gtnhlib.codec.stream;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagEnd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

final class ByteBufHelpers {

    /** Caps preallocation when decoding collections, since the declared count is untrusted. */
    static final int MAX_INITIAL_COLLECTION_SIZE = 65536;
    static final long DEFAULT_NBT_QUOTA = 2097152L;

    private ByteBufHelpers() {}

    static int readCount(ByteBuf input, int maxSize) {
        int count = VarInt.read(input);
        if (count < 0) {
            throw new DecoderException("Collection size " + count + " is negative");
        }
        if (count > maxSize) {
            throw new DecoderException("Collection of " + count + " elements exceeds the limit of " + maxSize);
        }
        return count;
    }

    static void writeCount(ByteBuf output, int count, int maxSize) {
        if (count > maxSize) {
            throw new EncoderException("Collection of " + count + " elements exceeds the limit of " + maxSize);
        }
        VarInt.write(output, count);
    }

    static NBTSizeTracker defaultNbtQuota() {
        return new NBTSizeTracker(DEFAULT_NBT_QUOTA);
    }

    static NBTSizeTracker unlimitedNbtHeap() {
        return NBTSizeTracker.field_152451_a;
    }

    static byte[] readByteArray(ByteBuf input, int maxSize) {
        int length = VarInt.read(input);
        if (length > maxSize) {
            throw new DecoderException("Byte array of " + length + " bytes exceeds the limit of " + maxSize);
        }
        if (length < 0 || length > input.readableBytes()) {
            throw new DecoderException(
                    "Byte array of " + length + " bytes does not fit in the " + input.readableBytes() + " bytes left");
        }
        byte[] data = new byte[length];
        input.readBytes(data);
        return data;
    }

    static void writeByteArray(ByteBuf output, byte[] value) {
        VarInt.write(output, value.length);
        output.writeBytes(value);
    }

    static long[] readLongArray(ByteBuf input) {
        int length = VarInt.read(input);
        if (length < 0 || length > input.readableBytes() / Long.BYTES) {
            throw new DecoderException(
                    "Long array of " + length
                            + " entries does not fit in the "
                            + input.readableBytes()
                            + " bytes left");
        }
        long[] values = new long[length];
        for (int i = 0; i < length; i++) {
            values[i] = input.readLong();
        }
        return values;
    }

    static void writeLongArray(ByteBuf output, long[] value) {
        VarInt.write(output, value.length);
        for (long entry : value) {
            output.writeLong(entry);
        }
    }

    static NBTBase readNbt(ByteBuf input, NBTSizeTracker accounter) {
        NBTBase tag;
        try {
            tag = CompressedStreamTools.func_152455_a(new ByteBufInputStream(input), 0, accounter);
        } catch (IOException e) {
            throw new DecoderException(e);
        }
        return tag instanceof NBTTagEnd ? null : tag;
    }

    static NBTTagCompound readCompoundNbt(ByteBuf input, NBTSizeTracker accounter) {
        NBTBase tag = readNbt(input, accounter);
        if (tag != null && !(tag instanceof NBTTagCompound)) {
            throw new DecoderException("Expected a compound tag but got " + tag);
        }
        return (NBTTagCompound) tag;
    }

    static void writeNbt(ByteBuf output, NBTBase tag) {
        try {
            CompressedStreamTools.func_150663_a(tag == null ? new NBTTagEnd() : tag, new ByteBufOutputStream(output));
        } catch (IOException e) {
            throw new EncoderException(e);
        }
    }

    static <T> T readNullable(ByteBuf input, Function<ByteBuf, T> reader) {
        return input.readBoolean() ? reader.apply(input) : null;
    }

    static <T> void writeNullable(ByteBuf output, T value, BiConsumer<ByteBuf, T> writer) {
        output.writeBoolean(value != null);
        if (value != null) {
            writer.accept(output, value);
        }
    }
}
