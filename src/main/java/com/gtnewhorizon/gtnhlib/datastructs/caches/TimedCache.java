package com.gtnewhorizon.gtnhlib.datastructs.caches;

import java.time.Duration;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@SuppressWarnings("unused")
public class TimedCache<K, V> {

    private static class Entry<K, V> {

        public final K key;
        public final int keyHash;
        public final V value;
        public long lastAccess;

        public Entry(K key, V value, long lastAccess) {
            this.key = key;
            this.keyHash = key.hashCode();
            this.value = value;
            this.lastAccess = lastAccess;
        }

        @Override
        public int hashCode() {
            return keyHash;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TimedCache.Entry<?, ?>e)) return false;

            return key.equals(e.key);
        }
    }

    private static class Generation<K, V> {

        public final long expiration;
        public final ObjectOpenHashSet<Entry<K, V>> entries = new ObjectOpenHashSet<>();
        public long lastCleanup;

        public Generation(long expiration) {
            this.expiration = expiration;
        }
    }

    private final Object2ObjectOpenHashMap<K, Entry<K, V>> values = new Object2ObjectOpenHashMap<>();

    private final Generation<K, V>[] generations;

    private final Function<K, V> fetcher;

    @Nullable
    private final BiConsumer<K, V> release;
    @Nullable
    private final Function<K, K> clone;

    public TimedCache(Function<K, V> fetcher, Duration gen0Timeout) {
        this(fetcher, getDefaultTimeouts(gen0Timeout), null, null);
    }

    public TimedCache(Function<K, V> fetcher, Duration[] generationTimeouts, @Nullable BiConsumer<K, V> release,
            @Nullable Function<K, K> clone) {
        this.fetcher = fetcher;
        this.release = release;
        this.clone = clone;

        // noinspection unchecked
        this.generations = new Generation[generationTimeouts.length];

        for (int i = 0; i < generationTimeouts.length; i++) {
            generations[i] = new Generation<>(generationTimeouts[i].toNanos());
        }
    }

    private static Duration[] getDefaultTimeouts(Duration gen0) {
        Duration[] genTimeouts = new Duration[3];

        genTimeouts[0] = gen0;
        genTimeouts[1] = Duration.ofNanos(gen0.toNanos() * 10);
        genTimeouts[2] = Duration.ofNanos(gen0.toNanos() * 100);

        return genTimeouts;
    }

    public final V get(K key) {
        long now = System.nanoTime();

        Entry<K, V> entry = values.get(key);

        if (entry == null) {
            doCleanup(now);

            if (clone != null) {
                key = clone.apply(key);
            }

            entry = new Entry<>(key, fetcher.apply(key), now);

            values.put(key, entry);
            generations[0].entries.add(entry);
        } else {
            entry.lastAccess = now;
        }

        return entry.value;
    }

    public final void doCleanup() {
        doCleanup(System.nanoTime());
    }

    private void doCleanup(long now) {
        if (values.isEmpty()) return;

        for (int i = 0; i < generations.length; i++) {
            Generation<K, V> generation = generations[i];

            if (generation.entries.isEmpty()) {
                generation.lastCleanup = now;
                continue;
            }

            if ((now - generation.lastCleanup) > generation.expiration) {
                generation.lastCleanup = now;

                Generation<K, V> nextGeneration = i < generations.length - 1 ? generations[i + 1] : null;

                for (Iterator<Entry<K, V>> iterator = generation.entries.iterator(); iterator.hasNext();) {
                    Entry<K, V> entry = iterator.next();

                    if (now - entry.lastAccess > generation.expiration) {
                        iterator.remove();
                        values.remove(entry.key);

                        if (release != null) release.accept(entry.key, entry.value);
                    } else if (nextGeneration != null) {
                        iterator.remove();
                        nextGeneration.entries.add(entry);
                    }
                }
            }
        }
    }

    public final void clear() {
        values.clear();

        for (Generation<K, V> generation : generations) {
            generation.entries.clear();
        }
    }
}
