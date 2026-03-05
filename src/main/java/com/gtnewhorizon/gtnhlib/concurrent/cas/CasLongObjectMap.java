package com.gtnewhorizon.gtnhlib.concurrent.cas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * A fast-to-read, slow-to-modify concurrent long→Object map, see {@link CasAdapter} for details.
 * <p>
 * Unlike {@link CasMap}, this uses primitive {@code long} keys (no boxing). The contract is: <b>do not mutate the
 * object returned by {@link #read()}</b>. There is no {@code Collections.unmodifiableMap} wrapper because that would
 * box keys on every access.
 * <p>
 * All convenience methods ({@link #get}, {@link #put}, etc.) are <b>lock-aware</b>: if the current thread holds the
 * bulk lock via {@link #lockAndGet()}, they operate directly on the locked mutable copy with zero copy overhead.
 *
 * @param <V> Value type
 */
@SuppressWarnings("unused") // API type
public class CasLongObjectMap<V> extends CasAdapter<Long2ObjectOpenHashMap<V>, Long2ObjectOpenHashMap<V>> {

    /** Constructs an empty map. */
    public CasLongObjectMap() {
        super(new Long2ObjectOpenHashMap<>());
    }

    @Override
    protected @NotNull Long2ObjectOpenHashMap<V> mutableCopyOf(@NotNull Long2ObjectOpenHashMap<V> data) {
        return data.clone();
    }

    @Override
    protected @NotNull Long2ObjectOpenHashMap<V> immutableCopyOf(@NotNull Long2ObjectOpenHashMap<V> data) {
        return data.clone();
    }

    private Long2ObjectOpenHashMap<V> readOrLocked() {
        final Long2ObjectOpenHashMap<V> locked = lockedMutable;
        if (locked != null && writeLock.isHeldByCurrentThread()) {
            return locked;
        }
        return read();
    }

    @Nullable
    public V get(long key) {
        return readOrLocked().get(key);
    }

    public boolean containsKey(long key) {
        return readOrLocked().containsKey(key);
    }

    public int size() {
        return readOrLocked().size();
    }

    public boolean isEmpty() {
        return readOrLocked().isEmpty();
    }

    @Nullable
    public V put(long key, V value) {
        final Long2ObjectOpenHashMap<V> locked = lockedMutable;
        if (locked != null && writeLock.isHeldByCurrentThread()) {
            return locked.put(key, value);
        }
        return mutate(m -> m.put(key, value));
    }

    @Nullable
    public V remove(long key) {
        final Long2ObjectOpenHashMap<V> locked = lockedMutable;
        if (locked != null && writeLock.isHeldByCurrentThread()) {
            return locked.remove(key);
        }
        return mutate(m -> m.remove(key));
    }

    /** @return A detached clone of the current snapshot, safe to mutate or pass to another thread. */
    public Long2ObjectOpenHashMap<V> snapshot() {
        return read().clone();
    }
}
