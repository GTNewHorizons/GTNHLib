package com.gtnewhorizon.gtnhlib.codec.stream;

import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.MAX_INITIAL_COLLECTION_SIZE;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.defaultNbtQuota;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.readByteArray;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.readCompoundNbt;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.readCount;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.readNbt;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.readNullable;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.writeByteArray;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.writeCount;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.writeNbt;
import static com.gtnewhorizon.gtnhlib.codec.stream.ByteBufHelpers.writeNullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.util.MathHelper;
import net.minecraft.util.RegistryNamespaced;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.gtnewhorizon.gtnhlib.codec.minecraft.NbtOps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public interface ByteBufCodecs {

    StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((buf, b) -> buf.writeByte(b), ByteBuf::readByte);
    StreamCodec<ByteBuf, Float> ROTATION_BYTE = BYTE.map(
            packed -> packed * 360.0F / 256.0F,
            degrees -> (byte) MathHelper.floor_float(degrees * 256.0F / 360.0F));
    StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of((buf, s) -> buf.writeShort(s), ByteBuf::readShort);
    StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = StreamCodec.of(ByteBuf::writeShort, ByteBuf::readUnsignedShort);
    StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(VarInt::write, VarInt::read);
    StreamCodec<ByteBuf, OptionalInt> OPTIONAL_VAR_INT = VAR_INT.map(
            raw -> raw == 0 ? OptionalInt.empty() : OptionalInt.of(raw - 1),
            opt -> opt.isPresent() ? opt.getAsInt() + 1 : 0);
    StreamCodec<ByteBuf, Long> LONG = StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong);
    StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(VarLong::write, VarLong::read);
    StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec
            .of(ByteBufHelpers::writeByteArray, buf -> readByteArray(buf, buf.readableBytes()));
    StreamCodec<ByteBuf, long[]> LONG_ARRAY = StreamCodec
            .of(ByteBufHelpers::writeLongArray, ByteBufHelpers::readLongArray);
    StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    StreamCodec<ByteBuf, NBTBase> TAG = tagCodec(ByteBufHelpers::defaultNbtQuota);
    StreamCodec<ByteBuf, NBTBase> TRUSTED_TAG = tagCodec(ByteBufHelpers::unlimitedNbtHeap);
    StreamCodec<ByteBuf, NBTTagCompound> COMPOUND_TAG = compoundTagCodec(ByteBufHelpers::defaultNbtQuota);
    StreamCodec<ByteBuf, NBTTagCompound> TRUSTED_COMPOUND_TAG = compoundTagCodec(ByteBufHelpers::unlimitedNbtHeap);
    StreamCodec<ByteBuf, Optional<NBTTagCompound>> OPTIONAL_COMPOUND_TAG = StreamCodec.of(
            (buf, tag) -> writeNbt(buf, tag.orElse(null)),
            buf -> Optional.ofNullable(readCompoundNbt(buf, defaultNbtQuota())));
    StreamCodec<ByteBuf, Vector3fc> VECTOR3F = StreamCodec.of(
            (buf, vec) -> buf.writeFloat(vec.x()).writeFloat(vec.y()).writeFloat(vec.z()),
            buf -> new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()));
    StreamCodec<ByteBuf, Quaternionfc> QUATERNIONF = StreamCodec.of(
            (buf, rot) -> buf.writeFloat(rot.x()).writeFloat(rot.y()).writeFloat(rot.z()).writeFloat(rot.w()),
            buf -> new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
    StreamCodec<ByteBuf, Integer> CONTAINER_ID = VAR_INT;
    StreamCodec<ByteBuf, UUID> UUID = StreamCodec.of(
            (buf, id) -> buf.writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits()),
            buf -> new UUID(buf.readLong(), buf.readLong()));
    StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = StreamCodec.of((buf, props) -> {
        writeCount(buf, props.size(), 16);
        props.values().forEach(prop -> {
            Utf8String.write(buf, prop.getName(), 64);
            Utf8String.write(buf, prop.getValue(), 32767);
            writeNullable(buf, prop.getSignature(), (out, sig) -> Utf8String.write(out, sig, 1024));
        });
    }, buf -> {
        PropertyMap props = new PropertyMap();
        for (int remaining = readCount(buf, 16); remaining > 0; remaining--) {
            String key = Utf8String.read(buf, 64);
            props.put(
                    key,
                    new Property(key, Utf8String.read(buf, 32767), readNullable(buf, in -> Utf8String.read(in, 1024))));
        }
        return props;
    });
    StreamCodec<ByteBuf, String> PLAYER_NAME = stringUtf8(16);
    StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = StreamCodec.of((buf, profile) -> {
        writeNullable(buf, profile.getId(), (out, id) -> UUID.encode(out, id));
        writeNullable(buf, profile.getName(), (out, name) -> PLAYER_NAME.encode(out, name));
        GAME_PROFILE_PROPERTIES.encode(buf, profile.getProperties());
    }, buf -> {
        UUID id = readNullable(buf, in -> UUID.decode(in));
        String name = readNullable(buf, in -> PLAYER_NAME.decode(in));
        if (id == null && (name == null || name.trim().isEmpty())) {
            throw new DecoderException("Game profile has neither an id nor a name");
        }
        GameProfile profile = new GameProfile(id, name);
        profile.getProperties().putAll(GAME_PROFILE_PROPERTIES.decode(buf));
        return profile;
    });
    StreamCodec<ByteBuf, Integer> RGB_COLOR = StreamCodec.of(
            (buf, rgb) -> buf.writeByte(rgb >>> 16 & 0xFF).writeByte(rgb >>> 8 & 0xFF).writeByte(rgb & 0xFF),
            buf -> {
                int red = buf.readUnsignedByte();
                int green = buf.readUnsignedByte();
                int blue = buf.readUnsignedByte();
                return 0xFF << 24 | red << 16 | green << 8 | blue;
            });

    static StreamCodec<ByteBuf, byte[]> byteArray(int maxSize) {
        return StreamCodec.of((buf, data) -> {
            if (data.length > maxSize) {
                throw new EncoderException("Byte array of " + data.length + " bytes exceeds the limit of " + maxSize);
            }
            writeByteArray(buf, data);
        }, buf -> readByteArray(buf, maxSize));
    }

    static StreamCodec<ByteBuf, String> stringUtf8(int maxStringLength) {
        return StreamCodec.of(
                (buf, text) -> Utf8String.write(buf, text, maxStringLength),
                buf -> Utf8String.read(buf, maxStringLength));
    }

    static StreamCodec<ByteBuf, Optional<NBTBase>> optionalTagCodec(Supplier<NBTSizeTracker> accounter) {
        return StreamCodec.of(
                (buf, tag) -> writeNbt(buf, tag.orElse(null)),
                buf -> Optional.ofNullable(readNbt(buf, accounter.get())));
    }

    static StreamCodec<ByteBuf, NBTBase> tagCodec(Supplier<NBTSizeTracker> accounter) {
        return StreamCodec.of((buf, tag) -> {
            if (tag == null || tag instanceof NBTTagEnd) {
                throw new EncoderException("Expected an NBT tag but got " + tag);
            }
            writeNbt(buf, tag);
        }, buf -> {
            NBTBase tag = readNbt(buf, accounter.get());
            if (tag == null) {
                throw new DecoderException("Expected an NBT tag but found an end tag");
            }
            return tag;
        });
    }

    static StreamCodec<ByteBuf, NBTTagCompound> compoundTagCodec(Supplier<NBTSizeTracker> accounter) {
        return tagCodec(accounter).map(tag -> {
            if (!(tag instanceof NBTTagCompound)) {
                throw new DecoderException("Expected a compound tag but got " + tag);
            }
            return (NBTTagCompound) tag;
        }, compound -> compound);
    }

    /** No NBT quota. Server-to-client only. */
    static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> codec) {
        return fromCodec(codec, ByteBufHelpers::unlimitedNbtHeap);
    }

    /** Sent as NBT through {@link NbtOps}, within the default 2 MiB quota. */
    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return fromCodec(codec, ByteBufHelpers::defaultNbtQuota);
    }

    static <B, T, V> StreamCodec.CodecOperation<B, T, V> fromCodec(DynamicOps<T> ops, Codec<V> codec) {
        return format -> StreamCodec
                .of(
                        (buf, value) -> format.encode(
                                buf,
                                codec.encodeStart(ops, value).getOrThrow(
                                        error -> new EncoderException("Could not encode " + value + ": " + error))),
                        buf -> {
                            T data = format.decode(buf);
                            return codec.parse(ops, data).getOrThrow(
                                    error -> new DecoderException("Could not decode " + data + ": " + error));
                        });
    }

    static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec, Supplier<NBTSizeTracker> accounter) {
        return tagCodec(accounter).apply(fromCodec(NbtOps.INSTANCE, codec));
    }

    static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<? super B, V> original) {
        return StreamCodec.of((buf, value) -> {
            buf.writeBoolean(value.isPresent());
            value.ifPresent(present -> original.encode(buf, present));
        }, buf -> buf.readBoolean() ? Optional.of(original.decode(buf)) : Optional.empty());
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> constructor,
            StreamCodec<? super B, V> elementCodec) {
        return collection(constructor, elementCodec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> constructor,
            StreamCodec<? super B, V> elementCodec, int maxSize) {
        return StreamCodec.of((buf, items) -> {
            writeCount(buf, items.size(), maxSize);
            items.forEach(item -> elementCodec.encode(buf, item));
        }, buf -> {
            int count = readCount(buf, maxSize);
            C items = constructor.apply(Math.min(count, MAX_INITIAL_COLLECTION_SIZE));
            for (int remaining = count; remaining > 0; remaining--) {
                items.add(elementCodec.decode(buf));
            }
            return items;
        });
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(
            IntFunction<C> constructor) {
        return element -> collection(constructor, element);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return list(Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
        return element -> collection(ArrayList::new, element, maxSize);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> constructor,
            StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return map(constructor, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> constructor,
            StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec, int maxSize) {
        return StreamCodec.of((buf, entries) -> {
            writeCount(buf, entries.size(), maxSize);
            entries.forEach((k, v) -> {
                keyCodec.encode(buf, k);
                valueCodec.encode(buf, v);
            });
        }, buf -> {
            int count = readCount(buf, maxSize);
            M entries = constructor.apply(Math.min(count, MAX_INITIAL_COLLECTION_SIZE));
            for (int remaining = count; remaining > 0; remaining--) {
                K k = keyCodec.decode(buf);
                entries.put(k, valueCodec.decode(buf));
            }
            return entries;
        });
    }

    static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(StreamCodec<? super B, L> leftCodec,
            StreamCodec<? super B, R> rightCodec) {
        return StreamCodec.of((buf, value) -> value.map(l -> {
            buf.writeBoolean(true);
            leftCodec.encode(buf, l);
            return null;
        }, r -> {
            buf.writeBoolean(false);
            rightCodec.encode(buf, r);
            return null;
        }), buf -> buf.readBoolean() ? Either.left(leftCodec.decode(buf)) : Either.right(rightCodec.decode(buf)));
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, V> lengthPrefixed(int maxSize,
            BiFunction<B, ByteBuf, B> decorator) {
        return inner -> StreamCodec.of((buf, value) -> {
            B staging = decorator.apply(buf, buf.alloc().buffer());
            try {
                inner.encode(staging, value);
                int length = staging.readableBytes();
                if (length > maxSize) {
                    throw new EncoderException("Payload of " + length + " bytes exceeds the limit of " + maxSize);
                }
                VarInt.write(buf, length);
                buf.writeBytes(staging);
            } finally {
                staging.release();
            }
        }, buf -> {
            int length = VarInt.read(buf);
            if (length > maxSize) {
                throw new DecoderException("Payload of " + length + " bytes exceeds the limit of " + maxSize);
            }
            if (length < 0 || length > buf.readableBytes()) {
                throw new DecoderException(
                        "Payload of " + length + " bytes does not fit in the " + buf.readableBytes() + " bytes left");
            }
            return inner.decode(decorator.apply(buf, buf.readSlice(length)));
        });
    }

    static <V> StreamCodec.CodecOperation<ByteBuf, V, V> lengthPrefixed(int maxSize) {
        return lengthPrefixed(maxSize, (parent, child) -> child);
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(IntFunction<T> byId, ToIntFunction<T> toId) {
        return StreamCodec
                .of((buf, value) -> VarInt.write(buf, toId.applyAsInt(value)), buf -> byId.apply(VarInt.read(buf)));
    }

    @SuppressWarnings("unchecked")
    static <T> StreamCodec<ByteBuf, T> idMapper(RegistryNamespaced registry) {
        return idMapper(id -> {
            if (!registry.containsId(id)) {
                throw new IllegalArgumentException("Registry has no entry with id " + id);
            }
            return (T) registry.getObjectById(id);
        }, entry -> {
            int id = registry.getIDForObject(entry);
            if (id < 0) {
                throw new IllegalArgumentException(entry + " is not in the registry");
            }
            return id;
        });
    }

    static <B extends FriendlyByteBuf, V extends Enum<V>> StreamCodec<B, V> enumCodec(Class<V> enumClass) {
        return StreamCodec.of(FriendlyByteBuf::writeEnum, buf -> buf.readEnum(enumClass));
    }

    static StreamCodec<ByteBuf, JsonElement> lenientJson(int maxStringLength) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return StreamCodec.of((buf, json) -> Utf8String.write(buf, gson.toJson(json), maxStringLength), buf -> {
            String text = Utf8String.read(buf, maxStringLength);
            try {
                return new JsonParser().parse(text);
            } catch (JsonSyntaxException e) {
                throw new DecoderException("Invalid JSON payload", e);
            }
        });
    }
}
