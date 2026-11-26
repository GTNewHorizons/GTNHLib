package com.gtnewhorizon.gtnhlib.codec.stream;

@FunctionalInterface
public interface StreamMemberEncoder<O, T> {

    void encode(T value, O output);
}
