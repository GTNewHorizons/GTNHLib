package com.gtnewhorizon.gtnhlib.inventory;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Pure, Minecraft-free diff between two aggregate item-count snapshots. Keys are packed item identities; values are
 * total counts. Emits one delta per identity whose total changed. Uses fastutil {@code fastIterator} so iteration
 * reuses a single mutable entry instead of allocating one per element.
 */
public final class InventoryDiffer {

    @FunctionalInterface
    public interface DeltaConsumer {
        /** @param delta positive = added, negative = removed. */
        void accept(long key, int delta);
    }

    private InventoryDiffer() {}

    /**
     * @param previous prior snapshot; MUST use default return value 0 for absent keys (the default for
     *                 {@code Long2IntOpenHashMap}) — a custom default would corrupt deltas for new keys.
     * @param current  current snapshot, same default-return-0 requirement.
     * @param consumer receives one signed delta per identity whose total changed.
     */
    public static void diff(Long2IntMap previous, Long2IntMap current, DeltaConsumer consumer) {
        final ObjectIterator<Long2IntMap.Entry> ci = Long2IntMaps.fastIterator(current);
        while (ci.hasNext()) {
            final Long2IntMap.Entry e = ci.next();
            final long key = e.getLongKey();
            final int delta = e.getIntValue() - previous.get(key);
            if (delta != 0) consumer.accept(key, delta);
        }
        final ObjectIterator<Long2IntMap.Entry> pi = Long2IntMaps.fastIterator(previous);
        while (pi.hasNext()) {
            final Long2IntMap.Entry e = pi.next();
            final long key = e.getLongKey();
            if (!current.containsKey(key)) consumer.accept(key, -e.getIntValue());
        }
    }
}
