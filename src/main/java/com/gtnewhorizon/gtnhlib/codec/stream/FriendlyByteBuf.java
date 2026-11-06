package com.gtnewhorizon.gtnhlib.codec.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.mojang.datafixers.util.Either;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

/**
 * 1.21's FriendlyByteBuf API on top of {@link PacketBuffer}. Formats match {@link ByteBufCodecs}, not the inherited
 * {@code PacketBuffer} methods such as {@code readNBTTagCompoundFromBuffer}.
 */
public class FriendlyByteBuf extends PacketBuffer {

    public static final int MAX_STRING_LENGTH = 32767;

    public FriendlyByteBuf(ByteBuf source) {
        super(source);
    }

    public <T, C extends Collection<T>> C readCollection(IntFunction<C> constructor,
            StreamDecoder<? super FriendlyByteBuf, T> elementDecoder) {
        int count = ByteBufHelpers.readCount(this, Integer.MAX_VALUE);
        C items = constructor.apply(Math.min(count, ByteBufHelpers.MAX_INITIAL_COLLECTION_SIZE));
        for (int i = 0; i < count; i++) {
            items.add(elementDecoder.decode(this));
        }
        return items;
    }

    public <T> void writeCollection(Collection<T> items, StreamEncoder<? super FriendlyByteBuf, T> elementEncoder) {
        VarInt.write(this, items.size());
        for (T item : items) {
            elementEncoder.encode(this, item);
        }
    }

    public <T> List<T> readList(StreamDecoder<? super FriendlyByteBuf, T> elementDecoder) {
        return readCollection(ArrayList::new, elementDecoder);
    }

    public <T> T[] readArray(IntFunction<T[]> constructor, StreamDecoder<? super FriendlyByteBuf, T> elementDecoder) {
        return readList(elementDecoder).toArray(constructor.apply(0));
    }

    public <T> FriendlyByteBuf writeArray(T[] items, StreamEncoder<? super FriendlyByteBuf, T> elementEncoder) {
        VarInt.write(this, items.length);
        for (T item : items) {
            elementEncoder.encode(this, item);
        }
        return this;
    }

    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> constructor,
            StreamDecoder<? super FriendlyByteBuf, K> keyDecoder,
            StreamDecoder<? super FriendlyByteBuf, V> valueDecoder) {
        int count = ByteBufHelpers.readCount(this, Integer.MAX_VALUE);
        M entries = constructor.apply(Math.min(count, ByteBufHelpers.MAX_INITIAL_COLLECTION_SIZE));
        for (int i = 0; i < count; i++) {
            K key = keyDecoder.decode(this);
            entries.put(key, valueDecoder.decode(this));
        }
        return entries;
    }

    public <K, V> Map<K, V> readMap(StreamDecoder<? super FriendlyByteBuf, K> keyDecoder,
            StreamDecoder<? super FriendlyByteBuf, V> valueDecoder) {
        return readMap(HashMap::new, keyDecoder, valueDecoder);
    }

    public <K, V> void writeMap(Map<K, V> entries, StreamEncoder<? super FriendlyByteBuf, K> keyEncoder,
            StreamEncoder<? super FriendlyByteBuf, V> valueEncoder) {
        VarInt.write(this, entries.size());
        entries.forEach((key, value) -> {
            keyEncoder.encode(this, key);
            valueEncoder.encode(this, value);
        });
    }

    public <T> Optional<T> readOptional(StreamDecoder<? super FriendlyByteBuf, T> valueDecoder) {
        return readBoolean() ? Optional.of(valueDecoder.decode(this)) : Optional.empty();
    }

    public <T> void writeOptional(Optional<T> value, StreamEncoder<? super FriendlyByteBuf, T> valueEncoder) {
        writeBoolean(value.isPresent());
        value.ifPresent(present -> valueEncoder.encode(this, present));
    }

    public <T> T readNullable(StreamDecoder<? super FriendlyByteBuf, T> valueDecoder) {
        return readBoolean() ? valueDecoder.decode(this) : null;
    }

    public <T> void writeNullable(T value, StreamEncoder<? super FriendlyByteBuf, T> valueEncoder) {
        writeBoolean(value != null);
        if (value != null) {
            valueEncoder.encode(this, value);
        }
    }

    public <L, R> Either<L, R> readEither(StreamDecoder<? super FriendlyByteBuf, L> leftDecoder,
            StreamDecoder<? super FriendlyByteBuf, R> rightDecoder) {
        return readBoolean() ? Either.left(leftDecoder.decode(this)) : Either.right(rightDecoder.decode(this));
    }

    public <L, R> void writeEither(Either<L, R> value, StreamEncoder<? super FriendlyByteBuf, L> leftEncoder,
            StreamEncoder<? super FriendlyByteBuf, R> rightEncoder) {
        value.map(left -> {
            writeBoolean(true);
            leftEncoder.encode(this, left);
            return null;
        }, right -> {
            writeBoolean(false);
            rightEncoder.encode(this, right);
            return null;
        });
    }

    public byte[] readByteArray() {
        return readByteArray(readableBytes());
    }

    public byte[] readByteArray(int maxSize) {
        return ByteBufHelpers.readByteArray(this, maxSize);
    }

    public FriendlyByteBuf writeByteArray(byte[] value) {
        ByteBufHelpers.writeByteArray(this, value);
        return this;
    }

    public int[] readVarIntArray() {
        return readVarIntArray(readableBytes());
    }

    public int[] readVarIntArray(int maxSize) {
        int length = VarInt.read(this);
        if (length > maxSize) {
            throw new DecoderException("Int array of " + length + " entries exceeds the limit of " + maxSize);
        }
        if (length < 0 || length > readableBytes()) {
            throw new DecoderException(
                    "Int array of " + length + " entries does not fit in the " + readableBytes() + " bytes left");
        }
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = VarInt.read(this);
        }
        return values;
    }

    public FriendlyByteBuf writeVarIntArray(int[] value) {
        VarInt.write(this, value.length);
        for (int entry : value) {
            VarInt.write(this, entry);
        }
        return this;
    }

    public long[] readLongArray() {
        return ByteBufHelpers.readLongArray(this);
    }

    public FriendlyByteBuf writeLongArray(long[] value) {
        ByteBufHelpers.writeLongArray(this, value);
        return this;
    }

    public BlockPos readBlockPos() {
        return new BlockPos().set(readLong());
    }

    public FriendlyByteBuf writeBlockPos(BlockPos pos) {
        writeLong(pos.asLong());
        return this;
    }

    public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
        T[] constants = enumClass.getEnumConstants();
        int ordinal = VarInt.read(this);
        if (ordinal < 0 || ordinal >= constants.length) {
            throw new DecoderException("Invalid ordinal " + ordinal + " for " + enumClass.getName());
        }
        return constants[ordinal];
    }

    public FriendlyByteBuf writeEnum(Enum<?> value) {
        VarInt.write(this, value.ordinal());
        return this;
    }

    public <T> T readById(IntFunction<T> byId) {
        return byId.apply(VarInt.read(this));
    }

    public <T> FriendlyByteBuf writeById(ToIntFunction<T> toId, T value) {
        VarInt.write(this, toId.applyAsInt(value));
        return this;
    }

    public int readVarInt() {
        return VarInt.read(this);
    }

    public FriendlyByteBuf writeVarInt(int value) {
        VarInt.write(this, value);
        return this;
    }

    public long readVarLong() {
        return VarLong.read(this);
    }

    public FriendlyByteBuf writeVarLong(long value) {
        VarLong.write(this, value);
        return this;
    }

    public UUID readUUID() {
        return ByteBufCodecs.UUID.decode(this);
    }

    public FriendlyByteBuf writeUUID(UUID value) {
        ByteBufCodecs.UUID.encode(this, value);
        return this;
    }

    public NBTTagCompound readNbt() {
        return ByteBufHelpers.readCompoundNbt(this, ByteBufHelpers.defaultNbtQuota());
    }

    public NBTBase readNbt(NBTSizeTracker accounter) {
        return ByteBufHelpers.readNbt(this, accounter);
    }

    public FriendlyByteBuf writeNbt(NBTBase tag) {
        ByteBufHelpers.writeNbt(this, tag);
        return this;
    }

    public String readUtf() {
        return readUtf(MAX_STRING_LENGTH);
    }

    public String readUtf(int maxLength) {
        return Utf8String.read(this, maxLength);
    }

    public FriendlyByteBuf writeUtf(String value) {
        return writeUtf(value, MAX_STRING_LENGTH);
    }

    public FriendlyByteBuf writeUtf(String value, int maxLength) {
        Utf8String.write(this, value, maxLength);
        return this;
    }

    public ResourceLocation readResourceLocation() {
        return new ResourceLocation(readUtf());
    }

    public FriendlyByteBuf writeResourceLocation(ResourceLocation value) {
        return writeUtf(value.toString());
    }
}
