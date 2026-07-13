package com.gtnewhorizon.gtnhlib.datastructs.space;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.pack;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackX;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackY;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackZ;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.functional.Compute3D;
import com.gtnewhorizon.gtnhlib.functional.Compute3DWithValue;
import com.gtnewhorizon.gtnhlib.functional.Consumer3DWithValue;
import com.gtnewhorizon.gtnhlib.space.XYZAddressable;

import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;

@SuppressWarnings("unused")
public class HashMap3D<V> extends Long2ObjectOpenHashMap<V> {

    public V get(int posX, int posY, int posZ) {
        return super.get(pack(posX, posY, posZ));
    }

    public V get(XYZAddressable xyz) {
        return get(xyz.getX(), xyz.getY(), xyz.getZ());
    }

    public V remove(int posX, int posY, int posZ) {
        return super.remove(pack(posX, posY, posZ));
    }

    public V remove(XYZAddressable xyz) {
        return remove(xyz.getX(), xyz.getY(), xyz.getZ());
    }

    public boolean containsKey(int posX, int posY, int posZ) {
        return super.containsKey(pack(posX, posY, posZ));
    }

    public boolean containsKey(XYZAddressable xyz) {
        return containsKey(xyz.getX(), xyz.getY(), xyz.getZ());
    }

    public V put(int posX, int posY, int posZ, V v) {
        return super.put(pack(posX, posY, posZ), v);
    }

    public V put(XYZAddressable xyz, V v) {
        return put(xyz.getX(), xyz.getY(), xyz.getZ(), v);
    }

    public V computeIfAbsent(int posX, int posY, int posZ, @NotNull Compute3D<V> mappingFunction) {
        V v;

        long key = pack(posX, posY, posZ);

        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(posX, posY, posZ)) != null) {
                put(key, newValue);
                return newValue;
            }
        }

        return v;
    }

    public V computeIfAbsent(XYZAddressable xyz, @NotNull Compute3D<V> mappingFunction) {
        return computeIfAbsent(xyz.getX(), xyz.getY(), xyz.getZ(), mappingFunction);
    }

    public V compute(int posX, int posY, int posZ, @NotNull Compute3DWithValue<V> remappingFunction) {
        return super.compute(pack(posX, posY, posZ), (k, v) -> remappingFunction.apply(posX, posY, posZ, v));
    }

    public V compute(XYZAddressable xyz, @NotNull Compute3DWithValue<V> remappingFunction) {
        return compute(xyz.getX(), xyz.getY(), xyz.getZ(), remappingFunction);
    }

    public V computeIfPresent(int posX, int posY, int posZ, @NotNull Compute3DWithValue<V> remappingFunction) {
        return super.computeIfPresent(pack(posX, posY, posZ), (k, v) -> remappingFunction.apply(posX, posY, posZ, v));
    }

    public V computeIfPresent(XYZAddressable xyz, @NotNull Compute3DWithValue<V> remappingFunction) {
        return computeIfPresent(xyz.getX(), xyz.getY(), xyz.getZ(), remappingFunction);
    }

    public V getOrDefault(int posX, int posY, int posZ, V defaultValue) {
        return super.getOrDefault(pack(posX, posY, posZ), defaultValue);
    }

    public V getOrDefault(XYZAddressable xyz, V defaultValue) {
        return getOrDefault(xyz.getX(), xyz.getY(), xyz.getZ(), defaultValue);
    }

    /// Unlike [java.util.Map#putIfAbsent], a key already mapped to `null` is treated as present and will not be
    /// overwritten.
    public V putIfAbsent(int posX, int posY, int posZ, V v) {
        return super.putIfAbsent(pack(posX, posY, posZ), v);
    }

    public V putIfAbsent(XYZAddressable xyz, V v) {
        return putIfAbsent(xyz.getX(), xyz.getY(), xyz.getZ(), v);
    }

    public boolean remove(int posX, int posY, int posZ, V value) {
        return super.remove(pack(posX, posY, posZ), value);
    }

    public boolean remove(XYZAddressable xyz, V value) {
        return remove(xyz.getX(), xyz.getY(), xyz.getZ(), value);
    }

    public V replace(int posX, int posY, int posZ, V value) {
        return super.replace(pack(posX, posY, posZ), value);
    }

    public V replace(XYZAddressable xyz, V value) {
        return replace(xyz.getX(), xyz.getY(), xyz.getZ(), value);
    }

    public boolean replace(int posX, int posY, int posZ, V oldValue, V newValue) {
        return super.replace(pack(posX, posY, posZ), oldValue, newValue);
    }

    public boolean replace(XYZAddressable xyz, V oldValue, V newValue) {
        return replace(xyz.getX(), xyz.getY(), xyz.getZ(), oldValue, newValue);
    }

    public V merge(int posX, int posY, int posZ, @NotNull V value,
            @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return super.merge(pack(posX, posY, posZ), value, remappingFunction);
    }

    public V merge(XYZAddressable xyz, @NotNull V value,
            @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(xyz.getX(), xyz.getY(), xyz.getZ(), value, remappingFunction);
    }

    public void forEach(Consumer3DWithValue<V> consumer) {
        for (var e : this.fastEntryIterable()) {
            consumer.accept(e.getX(), e.getY(), e.getZ(), e.getValue());
        }
    }

    public void replaceAll(@NotNull Compute3DWithValue<V> function) {
        var iter = Long2ObjectMaps.fastIterator(this);

        while (iter.hasNext()) {
            var e = iter.next();

            long key = e.getLongKey();
            V value = e.getValue();
            V newValue = function.apply(unpackX(key), unpackY(key), unpackZ(key), value);

            if (newValue != value) {
                e.setValue(newValue);
            }
        }
    }

    public interface FastEntrySet3D<V> extends ObjectSet<Entry3D<V>> {

        /**
         * Returns a fast iterator over this entry set; the iterator might return always the same entry instance,
         * suitably mutated.
         *
         * @return a fast iterator over this entry set; the iterator might return always the same
         *         {@link java.util.Map.Entry} instance, suitably mutated.
         */
        ObjectIterator<Entry3D<V>> fastIterator();

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
        default void fastForEach(final Consumer<? super Entry3D<V>> consumer) {
            forEach(consumer);
        }
    }

    private FastEntrySet3DImpl entrySet;

    public FastEntrySet3D<V> fastEntrySet() {
        if (entrySet == null) entrySet = new FastEntrySet3DImpl();

        return entrySet;
    }

    public Iterable<Entry3D<V>> fastEntryIterable() {
        return () -> fastEntrySet().fastIterator();
    }

    public Stream<Entry3D<V>> fastEntryStream() {
        return StreamSupport.stream(
                Spliterators.spliterator(
                        fastEntryIterable().iterator(),
                        size(),
                        Spliterator.SIZED | Spliterator.NONNULL | Spliterator.DISTINCT),
                false);
    }

    private class FastEntrySet3DImpl extends AbstractObjectSet<Entry3D<V>> implements FastEntrySet3D<V> {

        @Override
        public ObjectIterator<Entry3D<V>> fastIterator() {
            Entry3D<V> entry = new Entry3D<>();

            var iter = HashMap3D.this.long2ObjectEntrySet().fastIterator();

            return new ObjectIterator<>() {

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Entry3D<V> next() {
                    var e = iter.next();

                    entry.setKey(e.getLongKey());
                    entry.setValue(e.getValue());

                    return entry;
                }
            };
        }

        @Override
        public @NotNull ObjectIterator<Entry3D<V>> iterator() {
            var iter = HashMap3D.this.long2ObjectEntrySet().fastIterator();

            return new ObjectIterator<>() {

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Entry3D<V> next() {
                    var e = iter.next();

                    return new Entry3D<>(e.getLongKey(), e.getValue());
                }
            };
        }

        @Override
        public int size() {
            return HashMap3D.this.size();
        }
    }

    public static class Entry3D<T> extends BasicEntry<T> implements XYZAddressable {

        public Entry3D() {}

        /// @deprecated Use [#Entry3D(long, Object)] to avoid the long boxing.
        @Deprecated
        public Entry3D(Long key, T value) {
            super(key, value);
        }

        public Entry3D(long key, T value) {
            super(key, value);
        }

        public Entry3D(int posX, int posY, int posZ, T value) {
            super(pack(posX, posY, posZ), value);
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

        public final int getX() {
            return unpackX(getLongKey());
        }

        public final int getY() {
            return unpackY(getLongKey());
        }

        public final int getZ() {
            return unpackZ(getLongKey());
        }
    }
}
