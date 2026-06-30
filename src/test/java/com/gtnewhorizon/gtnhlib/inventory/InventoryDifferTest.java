package com.gtnewhorizon.gtnhlib.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

class InventoryDifferTest {

    private static Long2IntOpenHashMap map(long... kv) {
        Long2IntOpenHashMap m = new Long2IntOpenHashMap();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], (int) kv[i + 1]);
        return m;
    }

    /** Returns key -> delta, order-independent. */
    private static Long2IntOpenHashMap deltas(Long2IntOpenHashMap prev, Long2IntOpenHashMap cur) {
        Long2IntOpenHashMap out = new Long2IntOpenHashMap();
        InventoryDiffer.diff(prev, cur, out::put);
        return out;
    }

    @Test
    void sortingFiresNothing() {
        // identical totals (items just moved between slots) -> no deltas
        assertEquals(0, deltas(map(1, 64, 2, 10), map(1, 64, 2, 10)).size());
    }

    @Test
    void partialAdd() {
        Long2IntOpenHashMap d = deltas(map(1, 10), map(1, 15));
        assertEquals(1, d.size());
        assertEquals(5, d.get(1L));
    }

    @Test
    void partialRemove() {
        Long2IntOpenHashMap d = deltas(map(1, 15), map(1, 10));
        assertEquals(1, d.size());
        assertEquals(-5, d.get(1L));
    }

    @Test
    void fullRemove() {
        Long2IntOpenHashMap d = deltas(map(1, 10), map());
        assertEquals(1, d.size());
        assertEquals(-10, d.get(1L));
    }

    @Test
    void newItem() {
        Long2IntOpenHashMap d = deltas(map(), map(1, 10));
        assertEquals(1, d.size());
        assertEquals(10, d.get(1L));
    }

    @Test
    void typeChangeRemovesOldAddsNew() {
        Long2IntOpenHashMap d = deltas(map(1, 5), map(2, 5));
        assertEquals(2, d.size());
        assertEquals(-5, d.get(1L));
        assertEquals(5, d.get(2L));
    }

    @Test
    void gregtechSubtypesDiffIndependently() {
        // Same GT meta-item id, three subtypes by metadata. Gain one subtype, lose another, leave the third alone.
        final long matA = ItemIdentity.pack(4096, 1000, true);
        final long matB = ItemIdentity.pack(4096, 1001, true);
        final long matC = ItemIdentity.pack(4096, 1002, true);
        Long2IntOpenHashMap d = deltas(map(matA, 4, matB, 8, matC, 2), map(matA, 4, matB, 5, matC, 9));
        assertEquals(2, d.size()); // matA unchanged -> no entry
        assertEquals(-3, d.get(matB)); // lost 3 of subtype B
        assertEquals(7, d.get(matC)); // gained 7 of subtype C
    }
}
