package com.gtnewhorizon.gtnhlib.codec.stream;

import static com.gtnewhorizon.gtnhlib.codec.stream.StreamCodecTest.bytes;
import static com.gtnewhorizon.gtnhlib.codec.stream.StreamCodecTest.roundTrip;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.RegistryNamespaced;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Point;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Either;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

class ByteBufCodecsTest {

    @Test
    void varIntKnownEncodings() {
        assertArrayEquals(new byte[] { 0 }, bytes(ByteBufCodecs.VAR_INT, 0));
        assertArrayEquals(new byte[] { 0x7F }, bytes(ByteBufCodecs.VAR_INT, 127));
        assertArrayEquals(new byte[] { (byte) 0x80, 0x01 }, bytes(ByteBufCodecs.VAR_INT, 128));
        assertArrayEquals(new byte[] { (byte) 0xAC, 0x02 }, bytes(ByteBufCodecs.VAR_INT, 300));
        assertArrayEquals(
                new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F },
                bytes(ByteBufCodecs.VAR_INT, -1));
    }

    @Test
    void varIntRoundTripsAndSizes() {
        for (int value : new int[] { 0, 1, 127, 128, 16383, 16384, Integer.MAX_VALUE, Integer.MIN_VALUE, -1 }) {
            assertEquals(value, roundTrip(ByteBufCodecs.VAR_INT, value));
            assertEquals(bytes(ByteBufCodecs.VAR_INT, value).length, VarInt.getByteSize(value));
        }
    }

    @Test
    void varIntTooBigThrows() {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 6; i++) {
            buf.writeByte(0x80);
        }
        assertThrows(DecoderException.class, () -> VarInt.read(buf));

        ByteBuf longBuf = Unpooled.buffer();
        for (int i = 0; i < 11; i++) {
            longBuf.writeByte(0x80);
        }
        assertThrows(DecoderException.class, () -> VarLong.read(longBuf));
    }

    @Test
    void varLongRoundTripsAndSizes() {
        for (long value : new long[] { 0L, 1L, 127L, 128L, Long.MAX_VALUE, Long.MIN_VALUE, -1L }) {
            assertEquals(value, roundTrip(ByteBufCodecs.VAR_LONG, value));
            assertEquals(bytes(ByteBufCodecs.VAR_LONG, value).length, VarLong.getByteSize(value));
        }
        assertEquals(10, VarLong.getByteSize(-1L));
    }

    @Test
    void primitivesRoundTrip() {
        assertEquals(true, roundTrip(ByteBufCodecs.BOOL, true));
        assertEquals((byte) -5, roundTrip(ByteBufCodecs.BYTE, (byte) -5));
        assertEquals((short) -300, roundTrip(ByteBufCodecs.SHORT, (short) -300));
        assertEquals(65535, roundTrip(ByteBufCodecs.UNSIGNED_SHORT, 65535));
        assertEquals(-12345, roundTrip(ByteBufCodecs.INT, -12345));
        assertEquals(Long.MIN_VALUE, roundTrip(ByteBufCodecs.LONG, Long.MIN_VALUE));
        assertEquals(1.5F, roundTrip(ByteBufCodecs.FLOAT, 1.5F));
        assertEquals(-2.25D, roundTrip(ByteBufCodecs.DOUBLE, -2.25D));
        assertEquals(90.0F, roundTrip(ByteBufCodecs.ROTATION_BYTE, 90.0F));
        assertEquals(1, bytes(ByteBufCodecs.ROTATION_BYTE, 90.0F).length);
        assertEquals(5, roundTrip(ByteBufCodecs.CONTAINER_ID, 5));
    }

    @Test
    void optionalVarIntOffsetsByOne() {
        assertEquals(OptionalInt.empty(), roundTrip(ByteBufCodecs.OPTIONAL_VAR_INT, OptionalInt.empty()));
        assertEquals(OptionalInt.of(0), roundTrip(ByteBufCodecs.OPTIONAL_VAR_INT, OptionalInt.of(0)));
        assertArrayEquals(new byte[] { 0 }, bytes(ByteBufCodecs.OPTIONAL_VAR_INT, OptionalInt.empty()));
        assertArrayEquals(new byte[] { 1 }, bytes(ByteBufCodecs.OPTIONAL_VAR_INT, OptionalInt.of(0)));
    }

    @Test
    void arraysRoundTrip() {
        byte[] data = { 1, 2, 3 };
        assertArrayEquals(data, roundTrip(ByteBufCodecs.BYTE_ARRAY, data));
        long[] longs = { Long.MAX_VALUE, 0L, -7L };
        assertArrayEquals(longs, roundTrip(ByteBufCodecs.LONG_ARRAY, longs));
        assertThrows(EncoderException.class, () -> ByteBufCodecs.byteArray(2).encode(Unpooled.buffer(), data));
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.BYTE_ARRAY.encode(buf, data);
        assertThrows(DecoderException.class, () -> ByteBufCodecs.byteArray(2).decode(buf));
    }

    @Test
    void stringsRoundTripAndEnforceLimits() {
        String text = "héllo 世界 \uD834\uDD1E";
        assertEquals(text, roundTrip(ByteBufCodecs.STRING_UTF8, text));
        assertThrows(EncoderException.class, () -> ByteBufCodecs.stringUtf8(3).encode(Unpooled.buffer(), "four"));
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.STRING_UTF8.encode(buf, "four");
        assertThrows(DecoderException.class, () -> ByteBufCodecs.stringUtf8(3).decode(buf));
        assertThrows(
                EncoderException.class,
                () -> ByteBufCodecs.PLAYER_NAME.encode(Unpooled.buffer(), "xxxxxxxxxxxxxxxxx"));
    }

    @Test
    void stringLengthPrefixLongerThanBufferThrows() {
        ByteBuf buf = Unpooled.buffer();
        VarInt.write(buf, 10);
        buf.writeByte('a');
        assertThrows(DecoderException.class, () -> Utf8String.read(buf, 100));
    }

    @Test
    void optionalAndEither() {
        StreamCodec<ByteBuf, Optional<String>> optional = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8);
        assertEquals(Optional.of("a"), roundTrip(optional, Optional.of("a")));
        assertEquals(Optional.empty(), roundTrip(optional, Optional.empty()));
        assertArrayEquals(new byte[] { 0 }, bytes(optional, Optional.empty()));

        StreamCodec<ByteBuf, Either<Integer, String>> either = ByteBufCodecs
                .either(ByteBufCodecs.VAR_INT, ByteBufCodecs.STRING_UTF8);
        assertEquals(Either.left(4), roundTrip(either, Either.left(4)));
        assertEquals(Either.right("r"), roundTrip(either, Either.right("r")));
        assertEquals(1, bytes(either, Either.left(4))[0]);
        assertEquals(0, bytes(either, Either.right("r"))[0]);
    }

    @Test
    void collectionsAndMaps() {
        StreamCodec<ByteBuf, Set<Integer>> set = ByteBufCodecs.collection(LinkedHashSet::new, ByteBufCodecs.VAR_INT);
        Set<Integer> values = new LinkedHashSet<>(Arrays.asList(3, 1, 2));
        assertEquals(values, roundTrip(set, values));

        StreamCodec<ByteBuf, List<String>> limited = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(2));
        assertThrows(EncoderException.class, () -> limited.encode(Unpooled.buffer(), Arrays.asList("a", "b", "c")));
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, Arrays.asList("a", "b", "c"));
        assertThrows(DecoderException.class, () -> limited.decode(buf));

        StreamCodec<ByteBuf, Map<String, Integer>> map = ByteBufCodecs
                .map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT);
        Map<String, Integer> entries = new HashMap<>();
        entries.put("one", 1);
        entries.put("two", 2);
        assertEquals(entries, roundTrip(map, entries));

        StreamCodec<ByteBuf, ArrayList<Integer>> custom = ByteBufCodecs.VAR_INT
                .apply(ByteBufCodecs.collection(ArrayList::new));
        assertEquals(new ArrayList<>(Arrays.asList(5, 6)), roundTrip(custom, new ArrayList<>(Arrays.asList(5, 6))));

        ByteBuf negative = Unpooled.buffer();
        VarInt.write(negative, -1);
        assertThrows(DecoderException.class, () -> set.decode(negative));
    }

    @Test
    void lengthPrefixed() {
        StreamCodec<ByteBuf, String> codec = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.lengthPrefixed(100));
        assertEquals("payload", roundTrip(codec, "payload"));
        byte[] encoded = bytes(codec, "payload");
        assertEquals(encoded.length - 1, encoded[0]);
        StreamCodec<ByteBuf, String> tight = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.lengthPrefixed(3));
        assertThrows(EncoderException.class, () -> tight.encode(Unpooled.buffer(), "payload"));
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, "payload");
        assertThrows(DecoderException.class, () -> tight.decode(buf));

        ByteBuf truncated = Unpooled.buffer();
        VarInt.write(truncated, 50);
        assertThrows(DecoderException.class, () -> codec.decode(truncated));
        ByteBuf negative = Unpooled.buffer();
        VarInt.write(negative, -1);
        assertThrows(DecoderException.class, () -> codec.decode(negative));
    }

    @Test
    void idMapper() {
        List<String> ids = Arrays.asList("zero", "one", "two");
        StreamCodec<ByteBuf, String> codec = ByteBufCodecs.idMapper(ids::get, ids::indexOf);
        assertEquals("two", roundTrip(codec, "two"));
        assertArrayEquals(new byte[] { 2 }, bytes(codec, "two"));
    }

    @Test
    void idMapperRejectsUnknownRegistryIds() {
        RegistryNamespaced registry = new RegistryNamespaced();
        registry.addObject(3, "test:three", "three");
        StreamCodec<ByteBuf, String> codec = ByteBufCodecs.idMapper(registry);
        assertEquals("three", roundTrip(codec, "three"));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(Unpooled.buffer(), "missing"));
        ByteBuf buf = Unpooled.buffer();
        VarInt.write(buf, 7);
        assertThrows(IllegalArgumentException.class, () -> codec.decode(buf));
    }

    @Test
    void nbtTags() {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setInteger("i", 5);
        compound.setString("s", "text");
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString("entry"));
        compound.setTag("list", list);

        assertEquals(compound, roundTrip(ByteBufCodecs.COMPOUND_TAG, compound));
        assertEquals(compound, roundTrip(ByteBufCodecs.TRUSTED_COMPOUND_TAG, compound));
        NBTBase string = new NBTTagString("bare");
        assertEquals(string, roundTrip(ByteBufCodecs.TAG, string));
        assertEquals(Optional.of(compound), roundTrip(ByteBufCodecs.OPTIONAL_COMPOUND_TAG, Optional.of(compound)));
        assertEquals(Optional.empty(), roundTrip(ByteBufCodecs.OPTIONAL_COMPOUND_TAG, Optional.empty()));

        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.TAG.encode(buf, string);
        assertThrows(DecoderException.class, () -> ByteBufCodecs.COMPOUND_TAG.decode(buf));

        ByteBuf empty = Unpooled.buffer();
        ByteBufHelpers.writeNbt(empty, null);
        assertNull(ByteBufHelpers.readNbt(empty, ByteBufHelpers.defaultNbtQuota()));

        assertThrows(EncoderException.class, () -> ByteBufCodecs.TAG.encode(Unpooled.buffer(), null));
        assertThrows(EncoderException.class, () -> ByteBufCodecs.COMPOUND_TAG.encode(Unpooled.buffer(), null));
    }

    @Test
    void nbtQuotaRejectsOversizedTags() {
        NBTTagCompound big = new NBTTagCompound();
        big.setTag("data", new NBTTagByteArray(new byte[3_000_000]));
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(buf, big);
        assertThrows(RuntimeException.class, () -> ByteBufCodecs.COMPOUND_TAG.decode(buf.duplicate()));
        assertEquals(big, ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(buf));

        NBTTagCompound medium = new NBTTagCompound();
        medium.setTag("data", new NBTTagByteArray(new byte[1_000_000]));
        assertEquals(medium, roundTrip(ByteBufCodecs.COMPOUND_TAG, medium));
    }

    @Test
    void trustedTagRoundTrips() {
        NBTBase string = new NBTTagString("trusted");
        assertEquals(string, roundTrip(ByteBufCodecs.TRUSTED_TAG, string));
    }

    @Test
    void optionalTagCodec() {
        StreamCodec<ByteBuf, Optional<NBTBase>> codec = ByteBufCodecs.optionalTagCodec(ByteBufHelpers::defaultNbtQuota);
        NBTBase string = new NBTTagString("present");
        assertEquals(Optional.of(string), roundTrip(codec, Optional.of(string)));
        assertEquals(Optional.empty(), roundTrip(codec, Optional.empty()));
    }

    @Test
    void nullableHelpers() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufHelpers.<String>writeNullable(buf, null, (out, value) -> Utf8String.write(out, value, 10));
        ByteBufHelpers.writeNullable(buf, "x", (out, value) -> Utf8String.write(out, value, 10));
        assertNull(ByteBufHelpers.readNullable(buf, in -> Utf8String.read(in, 10)));
        assertEquals("x", ByteBufHelpers.readNullable(buf, in -> Utf8String.read(in, 10)));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void fromCodecGoesThroughNbt() {
        StreamCodec<ByteBuf, Point> codec = ByteBufCodecs.fromCodec(Point.CODEC);
        assertEquals(new Point(3, "p"), roundTrip(codec, new Point(3, "p")));
        assertEquals(new Point(4, "q"), roundTrip(ByteBufCodecs.fromCodecTrusted(Point.CODEC), new Point(4, "q")));

        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.COMPOUND_TAG.encode(buf, new NBTTagCompound());
        assertThrows(DecoderException.class, () -> codec.decode(buf));
    }

    @Test
    void jomlAndColor() {
        Vector3fc vector = new Vector3f(1, -2, 3.5F);
        assertEquals(vector, roundTrip(ByteBufCodecs.VECTOR3F, vector));
        Quaternionfc quaternion = new Quaternionf(0.1F, 0.2F, 0.3F, 0.9F);
        assertEquals(quaternion, roundTrip(ByteBufCodecs.QUATERNIONF, quaternion));
        assertEquals(0xFF123456, roundTrip(ByteBufCodecs.RGB_COLOR, 0x80123456));
        assertEquals(3, bytes(ByteBufCodecs.RGB_COLOR, 0x123456).length);
    }

    @Test
    void uuidAndGameProfile() {
        UUID id = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals(id, roundTrip(ByteBufCodecs.UUID, id));

        GameProfile profile = new GameProfile(id, "Notch");
        profile.getProperties().put("textures", new Property("textures", "value", "signature"));
        profile.getProperties().put("other", new Property("other", "v"));
        GameProfile result = roundTrip(ByteBufCodecs.GAME_PROFILE, profile);
        assertEquals(id, result.getId());
        assertEquals("Notch", result.getName());
        assertEquals(2, result.getProperties().size());
        Property textures = result.getProperties().get("textures").iterator().next();
        assertEquals("value", textures.getValue());
        assertEquals("signature", textures.getSignature());
        assertFalse(result.getProperties().get("other").iterator().next().hasSignature());
    }

    @Test
    void gameProfileWithMissingIdOrName() {
        GameProfile nameOnly = roundTrip(ByteBufCodecs.GAME_PROFILE, new GameProfile(null, "Notch"));
        assertNull(nameOnly.getId());
        assertEquals("Notch", nameOnly.getName());

        UUID id = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        GameProfile idOnly = roundTrip(ByteBufCodecs.GAME_PROFILE, new GameProfile(id, null));
        assertEquals(id, idOnly.getId());
        assertNull(idOnly.getName());

        ByteBuf buf = Unpooled.buffer();
        buf.writeBoolean(false);
        buf.writeBoolean(false);
        VarInt.write(buf, 0);
        assertThrows(DecoderException.class, () -> ByteBufCodecs.GAME_PROFILE.decode(buf));
    }

    @Test
    void eitherCarriesNullValues() {
        StreamCodec<ByteBuf, String> nullable = StreamCodec.of(
                (out, value) -> ByteBufHelpers.writeNullable(out, value, (o, v) -> Utf8String.write(o, v, 10)),
                in -> ByteBufHelpers.readNullable(in, i -> Utf8String.read(i, 10)));
        StreamCodec<ByteBuf, Either<String, String>> either = ByteBufCodecs.either(nullable, nullable);
        assertEquals(Either.left(null), roundTrip(either, Either.left(null)));
        assertEquals(Either.right(null), roundTrip(either, Either.right(null)));
    }

    @Test
    void lenientJson() {
        JsonObject object = new JsonObject();
        object.addProperty("key", "<value>");
        StreamCodec<ByteBuf, JsonElement> codec = ByteBufCodecs.lenientJson(1000);
        assertEquals(object, roundTrip(codec, object));

        ByteBuf buf = Unpooled.buffer();
        Utf8String.write(buf, "{key: unquoted}", 1000);
        assertTrue(codec.decode(buf).getAsJsonObject().has("key"));
    }
}
