package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;
import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.gtnewhorizon.gtnhlib.item.ItemStack2IntFunction;
import com.gtnewhorizon.gtnhlib.item.ItemStackPredicate;

/**
 * Something that can provide items. Should only be retrieved via
 * {@link CapabilityProvider#getCapability(Class, ForgeDirection)}. A source must be effectively stateless. That is, it
 * can use caches to improve performance but its methods must always reflect the state of the world immediately. There
 * is no defined lifetime for a source - it may last for one operation, or it may be stored across several ticks.
 */
public interface ItemSource {

    /**
     * Called once per transfer, before any configuration takes place. If this source a persistent object, its state
     * must be reset to match the world in this method.
     */
    default void resetSource() {

    }

    /**
     * Pulls a stack from this source. No guarantees are made that the source will be able to extract the whole amount -
     * some AE setups report more available items than are actually present.
     * 
     * @param filter The filter, or null for all items
     * @param amount The amount to pull, once an item is found
     * @return The first stack found, if any
     */
    @Nullable
    ItemStack pull(@Nullable ItemStackPredicate filter, @Nullable ItemStack2IntFunction amount);

    /**
     * Creates a new iterator for the items in this source. May return null if iterators are not supported. Modifying
     * any backing inventories while this iterator exists (without going through the iterator) is undefined behaviour.
     */
    @Nullable
    default InventoryIterator sourceIterator() {
        return null;
    }

    /**
     * Sets the slots that this source can pull from. If any given slot (or all of them) are invalid, they may be
     * ignored. The source must never crash, duplicate items, etc if a slot is invalid - it must be silently ignored.
     * <p />
     * If the source does not have something like a 'slot' (i.e. an ME system), the given slots can be ignored. If the
     * source does have slots, but the given slots are invalid (i.e. a single chest with the filter set to slot 100),
     * items must never be inserted.
     * <p />
     * When the argument is null, items can be pulled from any valid slot.
     */
    default void setAllowedSourceSlots(int @Nullable [] slots) {

    }
}
