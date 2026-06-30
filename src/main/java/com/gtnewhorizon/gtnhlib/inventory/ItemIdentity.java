package com.gtnewhorizon.gtnhlib.inventory;

/**
 * Packs item identity (id + metadata) into a single {@code long}. Id in high 32 bits, meta in low 32 bits: avoids
 * GregTech collisions even with large metadata. Meta is zeroed for non-subtype/damageable items. NBT excluded.
 */
public final class ItemIdentity {

    private ItemIdentity() {}

    public static long pack(int itemId, int meta, boolean hasSubtypes) {
        final int significantMeta = hasSubtypes ? meta : 0;
        return ((long) itemId << 32) | (significantMeta & 0xFFFFFFFFL);
    }

    public static int unpackId(long key) {
        return (int) (key >>> 32);
    }

    public static int unpackMeta(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }
}
