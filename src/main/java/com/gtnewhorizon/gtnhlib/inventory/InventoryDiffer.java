package com.gtnewhorizon.gtnhlib.inventory;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Diff between two item-count snapshots. Maps must use default-return-0 for absent keys.
 */
public final class InventoryDiffer {

    @FunctionalInterface
    public interface DeltaConsumer {

        /** @param delta positive = added, negative = removed. */
        void accept(long key, int delta);
    }

    private InventoryDiffer() {}

    /**
     * @param previous prior snapshot; MUST use default-return-0 for absent keys.
     * @param current  current snapshot; same default-return-0 requirement.
     * @param consumer receives one signed delta per identity whose count changed.
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
