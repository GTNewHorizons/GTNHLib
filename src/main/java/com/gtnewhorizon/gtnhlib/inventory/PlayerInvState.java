package com.gtnewhorizon.gtnhlib.inventory;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/** Per-player scan state. {@link #current} is cleared before each fill. */
public final class PlayerInvState {

    // Pre-sized so the first fills do not rehash.
    private static final int INITIAL_CAPACITY = 64;

    public Long2IntMap current = new Long2IntOpenHashMap(INITIAL_CAPACITY);
    public Long2IntMap previous = new Long2IntOpenHashMap(INITIAL_CAPACITY);
    public boolean seeded = false;
    public int ticksSinceScan = 0;

    /** After a diff, this scan's {@link #current} becomes {@link #previous} for next time. Does not clear. */
    public void swap() {
        final Long2IntMap tmp = current;
        current = previous;
        previous = tmp;
    }
}
