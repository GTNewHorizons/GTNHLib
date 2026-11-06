package com.gtnewhorizon.gtnhlib.codec.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Node;
import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Pair2;
import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Six;
import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Twelve;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class StreamCodecTest {

    static <V> V roundTrip(StreamCodec<? super ByteBuf, V> codec, V value) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        V result = codec.decode(buf);
        assertEquals(0, buf.readableBytes());
        return result;
    }

    static byte[] bytes(StreamCodec<? super ByteBuf, ?> codec, Object value) {
        ByteBuf buf = Unpooled.buffer();
        @SuppressWarnings("unchecked")
        StreamCodec<? super ByteBuf, Object> raw = (StreamCodec<? super ByteBuf, Object>) codec;
        raw.encode(buf, value);
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        return out;
    }

    @Test
    void ofUsesGivenEncoderAndDecoder() {
        StreamCodec<ByteBuf, Integer> codec = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
        assertEquals(42, roundTrip(codec, 42));
    }

    @Test
    void ofMemberSwapsArguments() {
        StreamCodec<ByteBuf, Pair2> codec = StreamCodec.ofMember((value, out) -> {
            out.writeInt(value.a());
            Utf8String.write(out, value.b(), 100);
        }, in -> new Pair2(in.readInt(), Utf8String.read(in, 100)));
        assertEquals(new Pair2(7, "x"), roundTrip(codec, new Pair2(7, "x")));
    }

    @Test
    void unitWritesNothingAndRejectsOtherValues() {
        StreamCodec<ByteBuf, String> codec = StreamCodec.unit("fixed");
        assertEquals(0, bytes(codec, "fixed").length);
        assertEquals("fixed", codec.decode(Unpooled.buffer()));
        assertThrows(IllegalStateException.class, () -> codec.encode(Unpooled.buffer(), "other"));
    }

    @Test
    void mapConvertsBothWays() {
        StreamCodec<ByteBuf, String> codec = ByteBufCodecs.INT.map(String::valueOf, Integer::parseInt);
        assertEquals("123", roundTrip(codec, "123"));
        assertArrayEquals(bytes(ByteBufCodecs.INT, 123), bytes(codec, "123"));
    }

    @Test
    void compositeTwoFields() {
        StreamCodec<ByteBuf, Pair2> codec = StreamCodec
                .composite(ByteBufCodecs.VAR_INT, Pair2::a, ByteBufCodecs.STRING_UTF8, Pair2::b, Pair2::new);
        assertEquals(new Pair2(300, "hello"), roundTrip(codec, new Pair2(300, "hello")));
    }

    @Test
    void compositeSixFieldsKeepsOrder() {
        StreamCodec<ByteBuf, Six> codec = StreamCodec.composite(
                ByteBufCodecs.INT,
                ((Six v) -> v.get(0)),
                ByteBufCodecs.INT,
                ((Six v) -> v.get(1)),
                ByteBufCodecs.INT,
                ((Six v) -> v.get(2)),
                ByteBufCodecs.INT,
                ((Six v) -> v.get(3)),
                ByteBufCodecs.INT,
                ((Six v) -> v.get(4)),
                ByteBufCodecs.INT,
                ((Six v) -> v.get(5)),
                Six::new);
        Six value = new Six(1, 2, 3, 4, 5, 6);
        assertEquals(value, roundTrip(codec, value));
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, buf.readInt());
        }
    }

    @Test
    void compositeTwelveFields() {
        StreamCodec<ByteBuf, Twelve> codec = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(0)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(1)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(2)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(3)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(4)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(5)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(6)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(7)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(8)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(9)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(10)),
                ByteBufCodecs.VAR_INT,
                ((Twelve v) -> v.get(11)),
                Twelve::new);
        Twelve value = new Twelve(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertEquals(value, roundTrip(codec, value));
    }

    @Test
    void dispatchWritesKeyThenValue() {
        StreamCodec<ByteBuf, Object> codec = ByteBufCodecs.STRING_UTF8.dispatch(
                value -> value instanceof Integer ? "int" : "string",
                key -> key.equals("int") ? ByteBufCodecs.VAR_INT : ByteBufCodecs.STRING_UTF8);
        assertEquals(5, roundTrip(codec, 5));
        assertEquals("abc", roundTrip(codec, "abc"));
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, 5);
        assertEquals("int", Utf8String.read(buf, 10));
        assertEquals(5, VarInt.read(buf));
    }

    @Test
    void recursiveHandlesNestedValues() {
        StreamCodec<ByteBuf, Node> codec = StreamCodec.recursive(
                self -> StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        Node::name,
                        self.apply(ByteBufCodecs.list()),
                        Node::children,
                        Node::new));
        Node tree = new Node(
                "root",
                Arrays.asList(
                        new Node("a", Collections.emptyList()),
                        new Node("b", Arrays.asList(new Node("c", Collections.emptyList())))));
        assertEquals(tree, roundTrip(codec, tree));
    }

    @Test
    void mapStreamWrapsBuffer() {
        List<ByteBuf> seen = new ArrayList<>();
        StreamCodec<ByteBuf, Integer> codec = ByteBufCodecs.INT.mapStream(buf -> {
            seen.add(buf);
            return buf;
        });
        assertEquals(9, roundTrip(codec, 9));
        assertEquals(2, seen.size());
    }

    @Test
    void castReturnsSameCodec() {
        StreamCodec<ByteBuf, Integer> codec = ByteBufCodecs.INT;
        assertSame(codec, codec.cast());
    }

    @Test
    void applyPassesThisCodec() {
        StreamCodec<ByteBuf, List<Integer>> codec = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());
        assertEquals(Arrays.asList(1, 2, 3), roundTrip(codec, Arrays.asList(1, 2, 3)));
        assertTrue(roundTrip(codec, Collections.emptyList()).isEmpty());
    }
}
