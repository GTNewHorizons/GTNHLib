package com.gtnewhorizon.gtnhlib.capability.item;

import java.util.OptionalInt;

import net.minecraft.item.ItemStack;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;

/**
 * Something that can accept items. Should only be retrieved via {@link CapabilityProvider#getCapability(Class, ForgeDirection)}.
 */
public interface IItemSink {

    /**
     * Injects a stack into this sink. This operation is not atomic. Any rejected items are returned. There is no
     * guarantee that the sink is completely full when items are rejected, this is just a best-effort operation.
     */
    ItemStack store(ItemStack stack);

    /**
     * Sets the slots that this sink can insert into. If any given slot (or all of them) are invalid, they may be
     * ignored. The sink must never crash, delete items, etc if a slot is invalid - it must be silently ignored.
     * <p />
     * If the sink does not have something like a 'slot' (i.e. an ME system), the given slots can be ignored. If the
     * sink does have slots, but the given slots are invalid (i.e. a single chest with the filter set to slot 100),
     * items must never be inserted.
     */
    default void setAllowedSinkSlots(int @Nullable [] slots) {

    }

    OptionalInt ZERO = OptionalInt.of(0);

    /**
     * Returns the stored amount of items in the sink. If this sink does not support this operation,
     * {@link OptionalInt#empty()} must be returned. Otherwise, a >=0 integer must be returned.
     * @param stack The stack, or null for all items.
     * @return The count for the given stack, or empty if this operation is invalid.
     */
    default OptionalInt getStoredAmount(@Nullable ItemStack stack) {
        return OptionalInt.empty();
    }

    /**
     * Chains one sink into another. Items are first fed into the current sink, then any rejects items are fed into the
     * next sink.
     */
    default IItemSink then(IItemSink next) {
        return (stack) -> {
            ItemStack rejected = this.store(stack);

            if (rejected == null) return null;

            return next.store(rejected);
        };
    }

    /**
     * A static version of {@link #then(IItemSink)} that supports arbitrary nulls. Will not allocate if either param is
     * null.
     */
    static IItemSink chain(IItemSink first, IItemSink second) {
        if (first == null || second == null) {
            return first != null ? first : second;
        }

        return first.then(second);
    }
}
