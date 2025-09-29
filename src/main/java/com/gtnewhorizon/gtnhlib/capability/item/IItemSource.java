package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;

/**
 * Something that can provide items. Should only be retrieved via
 * {@link CapabilityProvider#getCapability(Class, ForgeDirection)}.
 */
public interface IItemSource {

    /**
     * Creates a new iterator for the items in this source. The backing inventory may be modified while this iterator
     * exists, but no guarantees are made that the list will be updated to reflect them.
     */
    @NotNull
    InventorySourceIterator iterator();

    /**
     * Sets the slots that this source can pull from. If any given slot (or all of them) are invalid, they may be
     * ignored. The source must never crash, duplicate items, etc if a slot is invalid - it must be silently ignored.
     * <p />
     * If the source does not have something like a 'slot' (i.e. an ME system), the given slots can be ignored. If the
     * source does have slots, but the given slots are invalid (i.e. a single chest with the filter set to slot 100),
     * items must never be inserted.
     */
    default void setAllowedSourceSlots(int @Nullable [] slots) {

    }
}
