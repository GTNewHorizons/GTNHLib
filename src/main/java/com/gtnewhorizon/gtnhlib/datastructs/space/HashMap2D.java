package com.gtnewhorizon.gtnhlib.datastructs.space;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker2D.packChunk;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker2D.unpackChunkX;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker2D.unpackChunkZ;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.functional.Compute2D;
import com.gtnewhorizon.gtnhlib.functional.Compute2DWithValue;
import com.gtnewhorizon.gtnhlib.functional.Consumer2DWithValue;
import com.gtnewhorizon.gtnhlib.space.XZAddressable;

import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

@SuppressWarnings("unused")
public class HashMap2D<V> extends Long2ObjectOpenHashMap<V> {

    public V get(int posX, int posZ) {
        return super.get(packChunk(posX, posZ));
    }

    public V get(XZAddressable xyz) {
        return get(xyz.getX(), xyz.getZ());
    }

    public V remove(int posX, int posZ) {
        return super.remove(packChunk(posX, posZ));
    }

    public V remove(XZAddressable xyz) {
        return remove(xyz.getX(), xyz.getZ());
    }

    public boolean containsKey(int posX, int posZ) {
        return super.containsKey(packChunk(posX, posZ));
    }

    public boolean containsKey(XZAddressable xyz) {
        return containsKey(xyz.getX(), xyz.getZ());
    }

    public V put(int posX, int posZ, V v) {
        return super.put(packChunk(posX, posZ), v);
    }

    public V put(XZAddressable xyz, V v) {
        return put(xyz.getX(), xyz.getZ(), v);
    }

    public V computeIfAbsent(int posX, int posZ, @NotNull Compute2D<V> mappingFunction) {
        V v;

        long key = packChunk(posX, posZ);

        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(posX, posZ)) != null) {
                put(key, newValue);
                return newValue;
            }
        }

        return v;
    }

    public V computeIfAbsent(XZAddressable xyz, @NotNull Compute2D<V> mappingFunction) {
        return computeIfAbsent(xyz.getX(), xyz.getZ(), mappingFunction);
    }

    public V compute(int posX, int posZ, @NotNull Compute2DWithValue<V> remappingFunction) {
        return super.compute(packChunk(posX, posZ), (k, v) -> remappingFunction.apply(posX, posZ, v));
    }

    public V compute(XZAddressable xyz, @NotNull Compute2DWithValue<V> remappingFunction) {
        return compute(xyz.getX(), xyz.getZ(), remappingFunction);
    }

    public V computeIfPresent(int posX, int posZ, @NotNull Compute2DWithValue<V> remappingFunction) {
        return super.computeIfPresent(packChunk(posX, posZ), (k, v) -> remappingFunction.apply(posX, posZ, v));
    }

    public V computeIfPresent(XZAddressable xyz, @NotNull Compute2DWithValue<V> remappingFunction) {
        return computeIfPresent(xyz.getX(), xyz.getZ(), remappingFunction);
    }

    public V getOrDefault(int posX, int posZ, V defaultValue) {
        return super.getOrDefault(packChunk(posX, posZ), defaultValue);
    }

    public V getOrDefault(XZAddressable xyz, V defaultValue) {
        return getOrDefault(xyz.getX(), xyz.getZ(), defaultValue);
    }

    /// Unlike [java.util.Map#putIfAbsent], a key already mapped to `null` is treated as present and will not be
    /// overwritten.
    public V putIfAbsent(int posX, int posZ, V v) {
        return super.putIfAbsent(packChunk(posX, posZ), v);
    }

    public V putIfAbsent(XZAddressable xyz, V v) {
        return putIfAbsent(xyz.getX(), xyz.getZ(), v);
    }

    public boolean remove(int posX, int posZ, V value) {
        return super.remove(packChunk(posX, posZ), value);
    }

    public boolean remove(XZAddressable xyz, V value) {
        return remove(xyz.getX(), xyz.getZ(), value);
    }

    public V replace(int posX, int posZ, V value) {
        return super.replace(packChunk(posX, posZ), value);
    }

    public V replace(XZAddressable xyz, V value) {
        return replace(xyz.getX(), xyz.getZ(), value);
    }

    public boolean replace(int posX, int posZ, V oldValue, V newValue) {
        return super.replace(packChunk(posX, posZ), oldValue, newValue);
    }

    public boolean replace(XZAddressable xyz, V oldValue, V newValue) {
        return replace(xyz.getX(), xyz.getZ(), oldValue, newValue);
    }

    public V merge(int posX, int posZ, @NotNull V value,
            @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return super.merge(packChunk(posX, posZ), value, remappingFunction);
    }

    public V merge(XZAddressable xyz, @NotNull V value,
            @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(xyz.getX(), xyz.getZ(), value, remappingFunction);
    }

    public void forEach(Consumer2DWithValue<V> consumer) {
        for (var e : this.fastEntryIterable()) {
            consumer.accept(e.getX(), e.getZ(), e.getValue());
        }
    }

    public void replaceAll(@NotNull Compute2DWithValue<V> function) {
        var iter = Long2ObjectMaps.fastIterator(this);

        while (iter.hasNext()) {
            var e = iter.next();

            V value = e.getValue();
            V newValue = function.apply(unpackChunkX(e.getLongKey()), unpackChunkZ(e.getLongKey()), value);

            if (newValue != value) {
                e.setValue(newValue);
            }
        }
    }

    public interface FastEntrySet2D<V> extends ObjectSet<Entry2D<V>> {

        /**
         * Returns a fast iterator over this entry set; the iterator might return always the same entry instance,
         * suitably mutated.
         *
         * @return a fast iterator over this entry set; the iterator might return always the same
         *         {@link java.util.Map.Entry} instance, suitably mutated.
         */
        ObjectIterator<Entry2D<V>> fastIterator();

        /**
         * Iterates quickly over this entry set; the iteration might happen always on the same entry instance, suitably
         * mutated.
         *
         * <p>
         *
         * This default implementation just delegates to {@link #forEach(Consumer)}.
         *
         * @param consumer a consumer that will by applied to the entries of this set; the entries might be represented
         *                 by the same entry instance, suitably mutated.
         * @since 8.1.0
         */
        default void fastForEach(final Consumer<? super Entry2D<V>> consumer) {
            forEach(consumer);
        }
    }

    private FastEntrySet2DImpl entrySet;

    public FastEntrySet2D<V> fastEntrySet() {
        if (entrySet == null) entrySet = new FastEntrySet2DImpl();

        return entrySet;
    }

    public Iterable<Entry2D<V>> fastEntryIterable() {
        return () -> fastEntrySet().fastIterator();
    }

    public Stream<Entry2D<V>> fastEntryStream() {
        return StreamSupport.stream(
                Spliterators.spliterator(
                        fastEntryIterable().iterator(),
                        size(),
                        Spliterator.SIZED | Spliterator.NONNULL | Spliterator.DISTINCT),
                false);
    }

    private class FastEntrySet2DImpl extends AbstractObjectSet<Entry2D<V>> implements FastEntrySet2D<V> {

        @Override
        public ObjectIterator<Entry2D<V>> fastIterator() {
            Entry2D<V> entry = new Entry2D<>();

            var iter = HashMap2D.this.long2ObjectEntrySet().fastIterator();

            return new ObjectIterator<>() {

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Entry2D<V> next() {
                    var e = iter.next();

                    entry.setKey(e.getLongKey());
                    entry.setValue(e.getValue());

                    return entry;
                }
            };
        }

        @Override
        public @NotNull ObjectIterator<Entry2D<V>> iterator() {
            var iter = HashMap2D.this.long2ObjectEntrySet().fastIterator();

            return new ObjectIterator<>() {

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Entry2D<V> next() {
                    var e = iter.next();

                    return new Entry2D<>(e.getLongKey(), e.getValue());
                }
            };
        }

        @Override
        public int size() {
            return HashMap2D.this.size();
        }
    }

    public static class Entry2D<T> extends BasicEntry<T> implements XZAddressable {

        public Entry2D() {}

        /// @deprecated Use [#Entry2D(long, Object)] to avoid the long boxing.
        @Deprecated
        public Entry2D(Long key, T value) {
            super(key, value);
        }

        public Entry2D(long key, T value) {
            super(key, value);
        }

        public Entry2D(int posX, int posZ, T value) {
            super(packChunk(posX, posZ), value);
        }

        void setKey(long key) {
            super.key = key;
        }

        @Override
        public T setValue(T value) {
            T old = super.value;
            super.value = value;
            return old;
        }

        @Override
        public final int getX() {
            return unpackChunkX(getLongKey());
        }

        @Override
        public final int getZ() {
            return unpackChunkZ(getLongKey());
        }
    }
}
