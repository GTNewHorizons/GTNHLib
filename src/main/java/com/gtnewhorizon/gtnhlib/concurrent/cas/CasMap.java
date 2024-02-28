package com.gtnewhorizon.gtnhlib.concurrent.cas;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * A fast-to-read, slow-to-modify concurrent HashMap adapter, see {@link CasAdapter} for details on the implementation.
 * Use {@link CasMap#read()} and {@link CasMap#mutate} if you need to perform any sequence of operations, otherwise each
 * method call will operate on a different snapshot of the state of the map.
 *
 * @param <K> Map key type
 * @param <V> Map value type
 */
@SuppressWarnings("unused") // API type
public class CasMap<K, V> extends CasAdapter<Map<K, V>, Object2ObjectOpenHashMap<K, V>> implements Map<K, V> {

    /** Constructs an empty map */
    public CasMap() {
        super(Collections.unmodifiableMap(new Object2ObjectOpenHashMap<>()));
    }

    /** Constructs a new map with the given elements, or an empty map if null */
    public CasMap(@Nullable Object2ObjectOpenHashMap<K, V> map) {
        super(
                Collections.unmodifiableMap(
                        map == null ? new Object2ObjectOpenHashMap<>() : new Object2ObjectOpenHashMap<>(map)));
    }

    /** Constructs a new map with the given keys and values arrays, or an empty map if null */
    public CasMap(K @NotNull [] keys, V @NotNull [] values) {
        super(Collections.unmodifiableMap(new Object2ObjectOpenHashMap<>(keys, values)));
    }

    /** Constructs a map populated in the given lambda */
    public CasMap(final @NotNull Consumer<@NotNull Object2ObjectOpenHashMap<K, V>> constructor) {
        this();
        final Object2ObjectOpenHashMap<K, V> elems = new Object2ObjectOpenHashMap<>();
        constructor.accept(elems);
        overwrite(immutableCopyOf(elems));
    }

    @Override
    public final @NotNull Object2ObjectOpenHashMap<K, V> mutableCopyOf(@NotNull Map<K, V> data) {
        return new Object2ObjectOpenHashMap<>(data);
    }

    @Override
    public final @NotNull Map<K, V> immutableCopyOf(@NotNull Object2ObjectOpenHashMap<K, V> data) {
        // Protect against someone carrying a reference out of mutate() by copying again
        return Collections.unmodifiableMap(new Object2ObjectOpenHashMap<>(data));
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return read().getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        mutate(m -> {
            m.forEach(action);
            return null;
        });
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        mutate(m -> {
            m.replaceAll(function);
            return null;
        });
    }

    @Nullable
    @Override
    public V putIfAbsent(K key, V value) {
        final V oldValue = read().get(key);
        if (oldValue != null) {
            return oldValue;
        }
        return mutate(m -> m.putIfAbsent(key, value));
    }

    @Override
    public boolean remove(Object key, Object value) {
        @SuppressWarnings("SuspiciousMethodCalls")
        final V presentValue = read().get(key);
        if (!Objects.equals(value, presentValue)) {
            return false;
        }
        return mutate(m -> m.remove(key, value));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        final V presentValue = read().get(key);
        if (!Objects.equals(oldValue, presentValue)) {
            return false;
        }
        return mutate(m -> m.replace(key, oldValue, newValue));
    }

    @Nullable
    @Override
    public V replace(K key, V value) {
        return mutate(m -> m.replace(key, value));
    }

    @Override
    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        final V oldValue = read().get(key);
        if (oldValue != null) {
            return oldValue;
        }
        return mutate(m -> m.computeIfAbsent(key, mappingFunction));
    }

    @Override
    public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        final V oldValue = read().get(key);
        if (oldValue == null) {
            return null;
        }
        return mutate(m -> m.computeIfPresent(key, remappingFunction));
    }

    @Override
    public V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return mutate(m -> m.compute(key, remappingFunction));
    }

    @Override
    public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return mutate(m -> m.merge(key, value, remappingFunction));
    }

    @Override
    public int size() {
        return read().size();
    }

    @Override
    public boolean isEmpty() {
        return read().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return read().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return read().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return read().get(key);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        return mutate(m -> m.put(key, value));
    }

    @Override
    public V remove(Object key) {
        return mutate(m -> m.remove(key));
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> other) {
        mutate(m -> {
            m.putAll(other);
            return null;
        });
    }

    @Override
    public void clear() {
        mutate(m -> {
            m.clear();
            return null;
        });
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return read().keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return read().values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return read().entrySet();
    }

    @Override
    public int hashCode() {
        return read().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map)) {
            return false;
        }
        final Map<K, V> myMap = read();
        final Map<?, ?> otherMap = (obj instanceof CasMap) ? ((CasMap<?, ?>) obj).read() : (Map<?, ?>) obj;
        return myMap.equals(otherMap);
    }

    @Override
    public String toString() {
        return read().toString();
    }
}
