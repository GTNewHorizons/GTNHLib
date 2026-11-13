package com.gtnewhorizon.gtnhlib.codec.stream;

@FunctionalInterface
public interface StreamDecoder<I, T> {

    T decode(I input);
}
