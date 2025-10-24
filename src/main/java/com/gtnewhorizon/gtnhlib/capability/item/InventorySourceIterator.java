package com.gtnewhorizon.gtnhlib.capability.item;

import java.util.ListIterator;

import net.minecraft.item.ItemStack;

/**
 * It is assumed that this object is never stored in a field. It must not last for more than one discrete operation. The
 * backing data source (inventory, ME system, etc) can be modified, but no guarantees are made that the returned data is
 * complete or up to date beyond the guarantees laid out below.
 */
public interface InventorySourceIterator extends ListIterator<ImmutableItemStack> {

    InventorySourceIterator EMPTY = new EmptyInventorySourceIterator();

    @Override
    default void set(ImmutableItemStack immutableItemStack) {
        throw new UnsupportedOperationException("Cannot insert items into an InventorySourceIterator");
    }

    @Override
    default void add(ImmutableItemStack immutableItemStack) {
        throw new UnsupportedOperationException("Cannot insert items into an InventorySourceIterator");
    }

    @Override
    default void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove items from an InventorySourceIterator via remove(); use extract()");
    }

    /**
     * Extracts items from the current index. May return less items than what was originally reported for this index.
     *
     * @param amount The amount to extract
     * @return The extracted stack
     */
    ItemStack extract(int amount);

    /**
     * Returns the rejected items from a previously extracted stack. The item must be identical in every way, except the
     * stack size may be less than or equal to what was originally returned. Attempting to insert other stacks should do
     * nothing or throw an exception.
     */
    void insert(ItemStack stack);

    class EmptyInventorySourceIterator implements InventorySourceIterator {

        @Override
        public ItemStack extract(int amount) {
            return null;
        }

        @Override
        public void insert(ItemStack stack) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public ImmutableItemStack next() {
            return null;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public ImmutableItemStack previous() {
            return null;
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return 0;
        }
    }
}
