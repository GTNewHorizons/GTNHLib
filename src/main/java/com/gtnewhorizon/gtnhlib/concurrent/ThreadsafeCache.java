package com.gtnewhorizon.gtnhlib.concurrent;

import java.util.concurrent.locks.StampedLock;

import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;

/**
 * Borrowed from ModernFix, because 7.10 can't embed models in blocks easily
 */
public class ThreadsafeCache<K, V> {

    private final int size;
    private final Object2ReferenceLinkedOpenHashMap<K, V> cache = new Object2ReferenceLinkedOpenHashMap<>();
    private final StampedLock lock = new StampedLock();
    private final Function<K, V> modelRetriever;
    private final boolean allowNulls;

    public ThreadsafeCache(int size, Function<K, V> modelRetriever, boolean allowNulls) {
        this.size = size;
        this.modelRetriever = modelRetriever;
        this.allowNulls = allowNulls;
    }

    public void clear() {
        long stamp = lock.writeLock();
        try {
            cache.clear();
        } finally {
            lock.unlock(stamp);
        }
    }

    private boolean needToPopulate(K state) {
        long stamp = lock.readLock();
        try {
            return !cache.containsKey(state);
        } finally {
            lock.unlock(stamp);
        }
    }

    private V getModelFromCache(K state) {
        long stamp = lock.readLock();
        try {
            return cache.get(state);
        } finally {
            lock.unlock(stamp);
        }
    }

    private V cacheModel(K state) {
        V model = modelRetriever.apply(state);

        // Lock and modify our local, faster cache
        long stamp = lock.writeLock();

        try {
            cache.putAndMoveToFirst(state, model);
            // TODO: choose less arbitrary number
            if (cache.size() >= size) {
                cache.removeLast();
            }
        } finally {
            lock.unlock(stamp);
        }

        return model;
    }

    public V get(K key) {
        V model = getModelFromCache(key);

        if (model == null && (!allowNulls || needToPopulate(key))) {
            model = cacheModel(key);
        }

        return model;
    }
}
