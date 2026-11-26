package com.gtnewhorizon.gtnhlib.codec.stream;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

/** Prefixed with the UTF-8 byte length. {@code maxLength} counts chars. */
public final class Utf8String {

    private Utf8String() {}

    public static int maxEncodedLength(int maxLength) {
        return maxLength * 3;
    }

    public static String read(ByteBuf input, int maxLength) {
        int byteLimit = maxEncodedLength(maxLength);
        int byteCount = VarInt.read(input);
        if (byteCount > byteLimit) {
            throw new DecoderException("String of " + byteCount + " bytes exceeds the limit of " + byteLimit);
        }
        if (byteCount < 0) {
            throw new DecoderException("String length " + byteCount + " is negative");
        }
        if (byteCount > input.readableBytes()) {
            throw new DecoderException(
                    "String needs " + byteCount + " bytes but only " + input.readableBytes() + " are left");
        }
        String text = input.readSlice(byteCount).toString(StandardCharsets.UTF_8);
        if (text.length() > maxLength) {
            throw new DecoderException("String of " + text.length() + " characters exceeds the limit of " + maxLength);
        }
        return text;
    }

    public static void write(ByteBuf output, CharSequence value, int maxLength) {
        if (value.length() > maxLength) {
            throw new EncoderException("String of " + value.length() + " characters exceeds the limit of " + maxLength);
        }
        byte[] encoded = value.toString().getBytes(StandardCharsets.UTF_8);
        if (encoded.length > maxEncodedLength(maxLength)) {
            throw new EncoderException(
                    "String of " + encoded.length + " bytes exceeds the limit of " + maxEncodedLength(maxLength));
        }
        VarInt.write(output, encoded.length);
        output.writeBytes(encoded);
    }
}
