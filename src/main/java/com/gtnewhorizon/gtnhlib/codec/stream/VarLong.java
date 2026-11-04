package com.gtnewhorizon.gtnhlib.codec.stream;

import io.netty.buffer.ByteBuf;

public final class VarLong {

    public static final int MAX_VARLONG_SIZE = 10;
    private static final int PAYLOAD = 0x7F;
    private static final int MORE = 0x80;

    private VarLong() {}

    public static int getByteSize(long value) {
        int usedBits = Long.SIZE - Long.numberOfLeadingZeros(value);
        return usedBits == 0 ? 1 : (usedBits + 6) / 7;
    }

    public static boolean hasContinuationBit(byte in) {
        return (in & MORE) != 0;
    }

    public static long read(ByteBuf input) {
        long value = 0L;
        for (int index = 0; index < MAX_VARLONG_SIZE; index++) {
            byte part = input.readByte();
            value |= (long) (part & PAYLOAD) << (index * 7);
            if (!hasContinuationBit(part)) {
                return value;
            }
        }
        throw new RuntimeException("VarLong is longer than " + MAX_VARLONG_SIZE + " bytes");
    }

    public static ByteBuf write(ByteBuf output, long value) {
        long remaining = value;
        do {
            int part = (int) (remaining & PAYLOAD);
            remaining >>>= 7;
            output.writeByte(remaining != 0L ? part | MORE : part);
        } while (remaining != 0L);
        return output;
    }
}
