package com.gtnewhorizon.gtnhlib.codec.stream;

@FunctionalInterface
public interface StreamEncoder<O, T> {

    void encode(O output, T value);
}
