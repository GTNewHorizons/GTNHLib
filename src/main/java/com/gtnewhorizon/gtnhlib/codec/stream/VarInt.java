package com.gtnewhorizon.gtnhlib.codec.stream;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public final class VarInt {

    public static final int MAX_VARINT_SIZE = 5;
    private static final int PAYLOAD = 0x7F;
    private static final int MORE = 0x80;

    private VarInt() {}

    public static int getByteSize(int value) {
        int usedBits = Integer.SIZE - Integer.numberOfLeadingZeros(value);
        return usedBits == 0 ? 1 : (usedBits + 6) / 7;
    }

    public static boolean hasContinuationBit(byte in) {
        return (in & MORE) != 0;
    }

    public static int read(ByteBuf input) {
        int value = 0;
        for (int index = 0; index < MAX_VARINT_SIZE; index++) {
            byte part = input.readByte();
            value |= (part & PAYLOAD) << (index * 7);
            if (!hasContinuationBit(part)) {
                return value;
            }
        }
        throw new DecoderException("VarInt is longer than " + MAX_VARINT_SIZE + " bytes");
    }

    public static ByteBuf write(ByteBuf output, int value) {
        int remaining = value;
        do {
            int part = remaining & PAYLOAD;
            remaining >>>= 7;
            output.writeByte(remaining != 0 ? part | MORE : part);
        } while (remaining != 0);
        return output;
    }
}
