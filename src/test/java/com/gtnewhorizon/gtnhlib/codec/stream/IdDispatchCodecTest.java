package com.gtnewhorizon.gtnhlib.codec.stream;

import static com.gtnewhorizon.gtnhlib.codec.stream.StreamCodecTest.roundTrip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Chat;
import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Message;
import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Ping;
import com.gtnewhorizon.gtnhlib.codec.stream.TestValues.Unknown;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

class IdDispatchCodecTest {

    static class Undecorated extends RuntimeException implements IdDispatchCodec.DontDecorateException {
    }

    private static IdDispatchCodec<ByteBuf, Message, String> codec() {
        return IdDispatchCodec.<ByteBuf, Message, String>builder(Message::type)
                .add("ping", StreamCodec.composite(ByteBufCodecs.VAR_INT, Ping::id, Ping::new))
                .add("chat", StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Chat::text, Chat::new)).build();
    }

    @Test
    void idsFollowRegistrationOrder() {
        assertEquals(new Ping(7), roundTrip(codec(), new Ping(7)));
        assertEquals(new Chat("hi"), roundTrip(codec(), new Chat("hi")));

        ByteBuf buf = Unpooled.buffer();
        codec().encode(buf, new Chat("x"));
        assertEquals(1, VarInt.read(buf));
    }

    @Test
    void unknownTypeAndIdThrow() {
        assertThrows(EncoderException.class, () -> codec().encode(Unpooled.buffer(), new Unknown()));
        ByteBuf buf = Unpooled.buffer();
        VarInt.write(buf, 5);
        assertThrows(DecoderException.class, () -> codec().decode(buf));
    }

    @Test
    void duplicateTypeThrows() {
        IdDispatchCodec.Builder<ByteBuf, Message, String> builder = IdDispatchCodec
                .<ByteBuf, Message, String>builder(Message::type)
                .add("ping", StreamCodec.composite(ByteBufCodecs.VAR_INT, Ping::id, Ping::new))
                .add("ping", StreamCodec.composite(ByteBufCodecs.VAR_INT, Ping::id, Ping::new));
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void failuresAreWrappedUnlessMarked() {
        RuntimeException plain = new RuntimeException("plain");
        Undecorated marked = new Undecorated();
        IdDispatchCodec<ByteBuf, Message, String> failing = IdDispatchCodec
                .<ByteBuf, Message, String>builder(Message::type)
                .add("ping", StreamCodec.<ByteBuf, Ping>of((out, value) -> { throw plain; }, in -> { throw plain; }))
                .add("chat", StreamCodec.<ByteBuf, Chat>of((out, value) -> { throw marked; }, in -> { throw marked; }))
                .build();

        EncoderException wrapped = assertThrows(
                EncoderException.class,
                () -> failing.encode(Unpooled.buffer(), new Ping(1)));
        assertSame(plain, wrapped.getCause());
        assertSame(marked, assertThrows(Undecorated.class, () -> failing.encode(Unpooled.buffer(), new Chat("c"))));

        ByteBuf buf = Unpooled.buffer();
        VarInt.write(buf, 0);
        assertInstanceOf(DecoderException.class, assertThrows(RuntimeException.class, () -> failing.decode(buf)));
    }
}
