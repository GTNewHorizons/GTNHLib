package com.gtnewhorizon.gtnhlib.capability.item;

import java.util.OptionalInt;

import javax.annotation.Nonnegative;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.capability.CapabilityProvider;
import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;
import com.gtnewhorizon.gtnhlib.item.InsertionItemStack;
import com.gtnewhorizon.gtnhlib.item.InventoryIterator;
import com.gtnewhorizon.gtnhlib.item.ItemStackPredicate;

/**
 * Something that can accept items. Should only be retrieved via
 * {@link CapabilityProvider#getCapability(Class, ForgeDirection)}. A sink must be effectively stateless. That is, it
 * can use caches to improve performance but its methods must always reflect the state of the world immediately. There
 * is no defined lifetime for a sink - it may last for one operation, or it may be stored across several ticks.
 * Modifying the backing inventory while the sink is in use is undefined behaviour, though implementations should make a
 * best-effort to behave correctly if this interface is misused.
 */
public interface ItemSink {

    /**
     * Called once per transfer, before any configuration takes place. If this sink is a persistent object, its state
     * must be reset to match the world in this method.
     */
    default void resetSink() {

    }

    /**
     * Injects a stack into this sink. This operation is not atomic. There is no guarantee that the sink is completely
     * full when items are rejected, this is just a best-effort operation.
     *
     * @return The number of rejected items.
     */
    @Nonnegative
    int store(ImmutableItemStack stack);

    /**
     * Creates an iterator for this sink. May return null if iterators are not supported. Modifying any backing
     * inventories while this iterator exists (without going through the iterator) is undefined behaviour.
     */
    @Nullable
    default InventoryIterator sinkIterator() {
        return null;
    }

    /**
     * Sets the slots that this sink can insert into. If any given slot (or all of them) are invalid, they may be
     * ignored. The sink must never crash, delete items, etc if a slot is invalid - it must be silently ignored.
     * <p />
     * If the sink does not have something like a 'slot' (i.e. an ME system), the given slots can be ignored. If the
     * sink does have slots, but the given slots are invalid (i.e. a single chest with the filter set to slot 100),
     * items must never be inserted.
     * <p />
     * When the argument is null, items can be inserted into any valid slot.
     */
    default void setAllowedSinkSlots(int @Nullable [] slots) {

    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    OptionalInt ZERO = OptionalInt.of(0);

    /**
     * Returns the stored amount of items in the sink. If this sink does not support this operation,
     * {@link OptionalInt#empty()} must be returned. Otherwise, a >=0 integer must be returned.
     *
     * @param filter The filter, or null for all items
     * @return The count for the given stack, or {@link OptionalInt#empty()} if polling amounts is not supported.
     */
    default OptionalInt getStoredItemsInSink(@Nullable ItemStackPredicate filter) {
        InventoryIterator iter = sinkIterator();

        if (iter == null) return OptionalInt.empty();

        long total = 0;

        while (iter.hasNext()) {
            ImmutableItemStack stack = iter.next();

            if (stack == null) continue;

            if (filter != null && !filter.test(stack)) continue;

            total += stack.getStackSize();
        }

        return total == 0 ? ZERO : OptionalInt.of((int) Math.min(Integer.MAX_VALUE, total));
    }

    /**
     * Chains one sink into another. Items are first fed into the current sink, then any rejects items are fed into the
     * next sink.
     */
    default ItemSink then(ItemSink next) {
        return (stack) -> {
            int rejected = this.store(stack);

            if (rejected == 0) return 0;

            return next.store(new InsertionItemStack(stack, rejected));
        };
    }

    /**
     * A static version of {@link #then(ItemSink)} that supports arbitrary nulls. Will not allocate if either param is
     * null.
     */
    static ItemSink chain(ItemSink first, ItemSink second) {
        if (first == null || second == null) {
            return first != null ? first : second;
        }

        return first.then(second);
    }
}
