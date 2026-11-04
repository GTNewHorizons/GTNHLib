package com.gtnewhorizon.gtnhlib.codec.stream;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;

import io.netty.buffer.ByteBuf;

public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {

    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new SimpleStreamCodec<>(encoder, decoder);
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return of((buf, val) -> encoder.encode(val, buf), decoder);
    }

    static <B, V> StreamCodec<B, V> unit(V instance) {
        return of((buf, val) -> {
            if (!val.equals(instance)) {
                throw new IllegalStateException("Unit codec only accepts " + instance + " but was given " + val);
            }
        }, buf -> instance);
    }

    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> operation) {
        return operation.apply(this);
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        return of((buf, val) -> this.encode(buf, from.apply(val)), buf -> to.apply(this.decode(buf)));
    }

    default <O extends ByteBuf> StreamCodec<O, V> mapStream(Function<O, ? extends B> operation) {
        return of((buf, val) -> this.encode(operation.apply(buf), val), buf -> this.decode(operation.apply(buf)));
    }

    @SuppressWarnings("unchecked")
    default <U> StreamCodec<B, U> dispatch(Function<? super U, ? extends V> type,
            Function<? super V, ? extends StreamCodec<? super B, ? extends U>> codec) {
        return of((buf, val) -> {
            V tag = type.apply(val);
            this.encode(buf, tag);
            ((StreamCodec<? super B, U>) codec.apply(tag)).encode(buf, val);
        }, buf -> codec.apply(this.decode(buf)).decode(buf));
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
            Function<T1, C> constructor) {
        return new CompositeCodec<>(fields -> constructor.apply((T1) fields[0]), codec1, getter1);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
            StreamCodec<? super B, T2> codec2, Function<C, T2> getter2, BiFunction<T1, T2, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply((T1) fields[0], (T2) fields[1]),
                codec1,
                getter1,
                codec2,
                getter2);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1, Function<C, T1> getter1,
            StreamCodec<? super B, T2> codec2, Function<C, T2> getter2, StreamCodec<? super B, T3> codec3,
            Function<C, T3> getter3, Function3<T1, T2, T3, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply((T1) fields[0], (T2) fields[1], (T3) fields[2]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1,
            Function<C, T1> getter1, StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3, Function<C, T3> getter3, StreamCodec<? super B, T4> codec4,
            Function<C, T4> getter4, Function4<T1, T2, T3, T4, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply((T1) fields[0], (T2) fields[1], (T3) fields[2], (T4) fields[3]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1,
            Function<C, T1> getter1, StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3, Function<C, T3> getter3, StreamCodec<? super B, T4> codec4,
            Function<C, T4> getter4, StreamCodec<? super B, T5> codec5, Function<C, T5> getter5,
            Function5<T1, T2, T3, T4, T5, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor
                        .apply((T1) fields[0], (T2) fields[1], (T3) fields[2], (T4) fields[3], (T5) fields[4]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1,
            Function<C, T1> getter1, StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3, Function<C, T3> getter3, StreamCodec<? super B, T4> codec4,
            Function<C, T4> getter4, StreamCodec<? super B, T5> codec5, Function<C, T5> getter5,
            StreamCodec<? super B, T6> codec6, Function<C, T6> getter6,
            Function6<T1, T2, T3, T4, T5, T6, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply(
                        (T1) fields[0],
                        (T2) fields[1],
                        (T3) fields[2],
                        (T4) fields[3],
                        (T5) fields[4],
                        (T6) fields[5]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5,
                codec6,
                getter6);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1,
            Function<C, T1> getter1, StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3, Function<C, T3> getter3, StreamCodec<? super B, T4> codec4,
            Function<C, T4> getter4, StreamCodec<? super B, T5> codec5, Function<C, T5> getter5,
            StreamCodec<? super B, T6> codec6, Function<C, T6> getter6, StreamCodec<? super B, T7> codec7,
            Function<C, T7> getter7, Function7<T1, T2, T3, T4, T5, T6, T7, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply(
                        (T1) fields[0],
                        (T2) fields[1],
                        (T3) fields[2],
                        (T4) fields[3],
                        (T5) fields[4],
                        (T6) fields[5],
                        (T7) fields[6]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5,
                codec6,
                getter6,
                codec7,
                getter7);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1,
            Function<C, T1> getter1, StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3, Function<C, T3> getter3, StreamCodec<? super B, T4> codec4,
            Function<C, T4> getter4, StreamCodec<? super B, T5> codec5, Function<C, T5> getter5,
            StreamCodec<? super B, T6> codec6, Function<C, T6> getter6, StreamCodec<? super B, T7> codec7,
            Function<C, T7> getter7, StreamCodec<? super B, T8> codec8, Function<C, T8> getter8,
            Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply(
                        (T1) fields[0],
                        (T2) fields[1],
                        (T3) fields[2],
                        (T4) fields[3],
                        (T5) fields[4],
                        (T6) fields[5],
                        (T7) fields[6],
                        (T8) fields[7]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5,
                codec6,
                getter6,
                codec7,
                getter7,
                codec8,
                getter8);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> StreamCodec<B, C> composite(StreamCodec<? super B, T1> codec1,
            Function<C, T1> getter1, StreamCodec<? super B, T2> codec2, Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3, Function<C, T3> getter3, StreamCodec<? super B, T4> codec4,
            Function<C, T4> getter4, StreamCodec<? super B, T5> codec5, Function<C, T5> getter5,
            StreamCodec<? super B, T6> codec6, Function<C, T6> getter6, StreamCodec<? super B, T7> codec7,
            Function<C, T7> getter7, StreamCodec<? super B, T8> codec8, Function<C, T8> getter8,
            StreamCodec<? super B, T9> codec9, Function<C, T9> getter9,
            Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply(
                        (T1) fields[0],
                        (T2) fields[1],
                        (T3) fields[2],
                        (T4) fields[3],
                        (T5) fields[4],
                        (T6) fields[5],
                        (T7) fields[6],
                        (T8) fields[7],
                        (T9) fields[8]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5,
                codec6,
                getter6,
                codec7,
                getter7,
                codec8,
                getter8,
                codec9,
                getter9);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> codec1, Function<C, T1> getter1, StreamCodec<? super B, T2> codec2,
            Function<C, T2> getter2, StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
            StreamCodec<? super B, T4> codec4, Function<C, T4> getter4, StreamCodec<? super B, T5> codec5,
            Function<C, T5> getter5, StreamCodec<? super B, T6> codec6, Function<C, T6> getter6,
            StreamCodec<? super B, T7> codec7, Function<C, T7> getter7, StreamCodec<? super B, T8> codec8,
            Function<C, T8> getter8, StreamCodec<? super B, T9> codec9, Function<C, T9> getter9,
            StreamCodec<? super B, T10> codec10, Function<C, T10> getter10,
            Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply(
                        (T1) fields[0],
                        (T2) fields[1],
                        (T3) fields[2],
                        (T4) fields[3],
                        (T5) fields[4],
                        (T6) fields[5],
                        (T7) fields[6],
                        (T8) fields[7],
                        (T9) fields[8],
                        (T10) fields[9]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5,
                codec6,
                getter6,
                codec7,
                getter7,
                codec8,
                getter8,
                codec9,
                getter9,
                codec10,
                getter10);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> codec1, Function<C, T1> getter1, StreamCodec<? super B, T2> codec2,
            Function<C, T2> getter2, StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
            StreamCodec<? super B, T4> codec4, Function<C, T4> getter4, StreamCodec<? super B, T5> codec5,
            Function<C, T5> getter5, StreamCodec<? super B, T6> codec6, Function<C, T6> getter6,
            StreamCodec<? super B, T7> codec7, Function<C, T7> getter7, StreamCodec<? super B, T8> codec8,
            Function<C, T8> getter8, StreamCodec<? super B, T9> codec9, Function<C, T9> getter9,
            StreamCodec<? super B, T10> codec10, Function<C, T10> getter10, StreamCodec<? super B, T11> codec11,
            Function<C, T11> getter11, Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply(
                        (T1) fields[0],
                        (T2) fields[1],
                        (T3) fields[2],
                        (T4) fields[3],
                        (T5) fields[4],
                        (T6) fields[5],
                        (T7) fields[6],
                        (T8) fields[7],
                        (T9) fields[8],
                        (T10) fields[9],
                        (T11) fields[10]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5,
                codec6,
                getter6,
                codec7,
                getter7,
                codec8,
                getter8,
                codec9,
                getter9,
                codec10,
                getter10,
                codec11,
                getter11);
    }

    @SuppressWarnings("unchecked")
    static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> codec1, Function<C, T1> getter1, StreamCodec<? super B, T2> codec2,
            Function<C, T2> getter2, StreamCodec<? super B, T3> codec3, Function<C, T3> getter3,
            StreamCodec<? super B, T4> codec4, Function<C, T4> getter4, StreamCodec<? super B, T5> codec5,
            Function<C, T5> getter5, StreamCodec<? super B, T6> codec6, Function<C, T6> getter6,
            StreamCodec<? super B, T7> codec7, Function<C, T7> getter7, StreamCodec<? super B, T8> codec8,
            Function<C, T8> getter8, StreamCodec<? super B, T9> codec9, Function<C, T9> getter9,
            StreamCodec<? super B, T10> codec10, Function<C, T10> getter10, StreamCodec<? super B, T11> codec11,
            Function<C, T11> getter11, StreamCodec<? super B, T12> codec12, Function<C, T12> getter12,
            Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> constructor) {
        return new CompositeCodec<>(
                fields -> constructor.apply(
                        (T1) fields[0],
                        (T2) fields[1],
                        (T3) fields[2],
                        (T4) fields[3],
                        (T5) fields[4],
                        (T6) fields[5],
                        (T7) fields[6],
                        (T8) fields[7],
                        (T9) fields[8],
                        (T10) fields[9],
                        (T11) fields[10],
                        (T12) fields[11]),
                codec1,
                getter1,
                codec2,
                getter2,
                codec3,
                getter3,
                codec4,
                getter4,
                codec5,
                getter5,
                codec6,
                getter6,
                codec7,
                getter7,
                codec8,
                getter8,
                codec9,
                getter9,
                codec10,
                getter10,
                codec11,
                getter11,
                codec12,
                getter12);
    }

    static <B, T> StreamCodec<B, T> recursive(UnaryOperator<StreamCodec<B, T>> factory) {
        return new RecursiveCodec<>(factory);
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {

        StreamCodec<B, T> apply(StreamCodec<B, S> original);
    }
}
