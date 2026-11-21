package com.gtnewhorizon.gtnhlib.codec.minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public interface StringRepresentable {

    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> EnumCodec<E> fromEnum(Supplier<E[]> values) {
        Map<String, E> byName = new HashMap<>();
        for (E value : values.get()) {
            String name = value.getSerializedName();
            E previous = byName.put(name, value);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate serialized name " + name + " for " + previous + " and " + value);
            }
        }
        return new EnumCodec<>(byName);
    }

    final class EnumCodec<E extends Enum<E> & StringRepresentable> implements Codec<E> {

        private final Map<String, E> byName;
        private final Codec<E> codec;

        private EnumCodec(Map<String, E> byName) {
            this.byName = byName;
            this.codec = Codec.STRING.comapFlatMap(name -> {
                E value = byName.get(name);
                return value != null ? DataResult.success(value) : DataResult.error(() -> "Unknown value: " + name);
            }, StringRepresentable::getSerializedName);
        }

        @Nullable
        public E byName(@Nullable String name) {
            return name == null ? null : byName.get(name);
        }

        public E byName(@Nullable String name, E fallback) {
            E value = byName(name);
            return value != null ? value : fallback;
        }

        @Override
        public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
            return codec.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
            return codec.encode(input, ops, prefix);
        }
    }
}
