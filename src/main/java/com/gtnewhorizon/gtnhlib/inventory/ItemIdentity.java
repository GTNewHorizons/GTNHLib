package com.gtnewhorizon.gtnhlib.inventory;

/**
 * Packs an item's identity (registry id + subtype metadata) into a single {@code long}, with no allocation. Used to
 * aggregate inventory contents by identity. NBT is intentionally not part of the identity.
 * <p>
 * The id occupies the high 32 bits and the metadata the low 32 bits, so even GregTech meta-items (one id, thousands
 * of high-valued metadata subtypes) never collide. Metadata is only significant for items that declare subtypes;
 * for damageable items (tools) it is zeroed so durability changes do not look like add/remove.
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
