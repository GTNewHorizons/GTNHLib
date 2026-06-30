package com.gtnewhorizon.gtnhlib.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ItemIdentityTest {

    // A GregTech meta-item: one item id, many subtypes by metadata.
    private static final int GT_META_ITEM_ID = 4096;

    @Test
    void distinctGregtechSubtypesGetDistinctKeys() {
        // e.g. two different GT materials/parts on the same meta-item id
        long a = ItemIdentity.pack(GT_META_ITEM_ID, 1000, true);
        long b = ItemIdentity.pack(GT_META_ITEM_ID, 1001, true);
        assertNotEquals(a, b);
    }

    @Test
    void highMetaRoundTrips() {
        // a real ItemStack metadata is stored as a short; 32767 is its max
        long key = ItemIdentity.pack(GT_META_ITEM_ID, 32767, true);
        assertEquals(GT_META_ITEM_ID, ItemIdentity.unpackId(key));
        assertEquals(32767, ItemIdentity.unpackMeta(key));
    }

    @Test
    void idAndMetaDoNotCollide() {
        // a high id with meta 0 must never equal a low id with a high meta
        assertNotEquals(ItemIdentity.pack(GT_META_ITEM_ID, 0, true), ItemIdentity.pack(0, GT_META_ITEM_ID, true));
    }

    @Test
    void damageableItemCollapsesMeta() {
        // hasSubtypes == false (a tool): durability/damage must NOT split identity
        long fresh = ItemIdentity.pack(7, 0, false);
        long worn = ItemIdentity.pack(7, 561, false);
        assertEquals(fresh, worn);
        assertEquals(0, ItemIdentity.unpackMeta(fresh));
    }

    @Test
    void subtypeItemKeepsMeta() {
        long key = ItemIdentity.pack(35, 14, true); // e.g. red wool
        assertEquals(14, ItemIdentity.unpackMeta(key));
    }

    @Test
    void metaBeyond16BitsRoundTrips() {
        // A 16-bit packing scheme would truncate this; pins id in the high 32 bits, meta in the low 32.
        long key = ItemIdentity.pack(1, 100000, true);
        assertEquals(1, ItemIdentity.unpackId(key));
        assertEquals(100000, ItemIdentity.unpackMeta(key));
    }
}
