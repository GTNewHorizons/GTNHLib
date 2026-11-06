package com.gtnewhorizon.gtnhlib.codec.stream;

import java.util.function.UnaryOperator;

final class RecursiveCodec<B, T> implements StreamCodec<B, T> {

    private final UnaryOperator<StreamCodec<B, T>> factory;
    private volatile StreamCodec<B, T> built;

    RecursiveCodec(UnaryOperator<StreamCodec<B, T>> factory) {
        this.factory = factory;
    }

    private StreamCodec<B, T> target() {
        StreamCodec<B, T> codec = built;
        if (codec == null) {
            synchronized (this) {
                codec = built;
                if (codec == null) {
                    codec = factory.apply(this);
                    built = codec;
                }
            }
        }
        return codec;
    }

    @Override
    public T decode(B input) {
        return target().decode(input);
    }

    @Override
    public void encode(B output, T value) {
        target().encode(output, value);
    }
}
