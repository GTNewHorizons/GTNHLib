package com.gtnewhorizon.gtnhlib.codec.stream;

final class SimpleStreamCodec<B, V> implements StreamCodec<B, V> {

    private final StreamEncoder<B, V> writer;
    private final StreamDecoder<B, V> reader;

    SimpleStreamCodec(StreamEncoder<B, V> writer, StreamDecoder<B, V> reader) {
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    public V decode(B input) {
        return reader.decode(input);
    }

    @Override
    public void encode(B output, V value) {
        writer.encode(output, value);
    }
}
