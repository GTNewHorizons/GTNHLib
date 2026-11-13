package com.gtnewhorizon.gtnhlib.codec.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

/** Ids are assigned in registration order. */
public class IdDispatchCodec<B extends ByteBuf, V, T> implements StreamCodec<B, V> {

    private final Function<V, ? extends T> classifier;
    private final List<T> types;
    private final List<StreamCodec<? super B, ? extends V>> codecs;
    private final Map<T, Integer> idsByType;

    private IdDispatchCodec(Function<V, ? extends T> classifier, List<T> types,
            List<StreamCodec<? super B, ? extends V>> codecs) {
        this.classifier = classifier;
        this.types = types;
        this.codecs = codecs;
        this.idsByType = new HashMap<>();
        for (int i = 0; i < types.size(); i++) {
            if (this.idsByType.put(types.get(i), i) != null) {
                throw new IllegalStateException("Type " + types.get(i) + " was registered more than once");
            }
        }
    }

    @Override
    public V decode(B input) {
        int id = VarInt.read(input);
        if (id < 0 || id >= codecs.size()) {
            throw new DecoderException("No packet type registered for id " + id);
        }
        try {
            return codecs.get(id).decode(input);
        } catch (RuntimeException e) {
            if (e instanceof DontDecorateException) {
                throw e;
            }
            throw new DecoderException("Could not read packet of type " + types.get(id), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode(B output, V value) {
        T type = classifier.apply(value);
        Integer id = idsByType.get(type);
        if (id == null) {
            throw new EncoderException("Packet type " + type + " is not registered");
        }
        VarInt.write(output, id);
        try {
            ((StreamCodec<? super B, V>) codecs.get(id)).encode(output, value);
        } catch (RuntimeException e) {
            if (e instanceof DontDecorateException) {
                throw e;
            }
            throw new EncoderException("Could not write packet of type " + type, e);
        }
    }

    public static <B extends ByteBuf, V, T> Builder<B, V, T> builder(Function<V, ? extends T> typeGetter) {
        return new Builder<>(typeGetter);
    }

    public static class Builder<B extends ByteBuf, V, T> {

        private final Function<V, ? extends T> classifier;
        private final List<T> types = new ArrayList<>();
        private final List<StreamCodec<? super B, ? extends V>> codecs = new ArrayList<>();

        private Builder(Function<V, ? extends T> classifier) {
            this.classifier = classifier;
        }

        public Builder<B, V, T> add(T type, StreamCodec<? super B, ? extends V> serializer) {
            types.add(type);
            codecs.add(serializer);
            return this;
        }

        public IdDispatchCodec<B, V, T> build() {
            return new IdDispatchCodec<>(classifier, new ArrayList<>(types), new ArrayList<>(codecs));
        }
    }

    /** Rethrown without wrapping. */
    public interface DontDecorateException {
    }
}
