package com.gtnewhorizon.gtnhlib.util.data;

import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;

/**
 * A mutable implementation of {@link ImmutableItemMeta}. If your API should return a mutable pair, return this instead.
 * Must follow the same contracts as the immutable version if this is ever upcast to a {@link ImmutableItemMeta} in your
 * API. If this type is exposed instead of the immutable interface, assume that the contained values can change.
 */
public class ItemMeta implements ImmutableItemMeta {

    @Nonnull
    private Item item;
    private int meta;

    public ItemMeta(@Nonnull Item item, int meta) {
        this.item = item;
        this.meta = meta;
    }

    public ItemMeta(@Nonnull Item item) {
        this(item, 0);
    }

    @Override
    @Nonnull
    public Item item() {
        return item;
    }

    @Override
    public int getItemMeta() {
        return meta;
    }

    /**
     * Note: see the header comment in {@link ImmutableItemMeta} for this method's contract.
     */
    public ItemMeta setItem(@Nonnull Item item) {
        this.item = Objects.requireNonNull(item);

        return this;
    }

    /**
     * Note: see the header comment in {@link ImmutableItemMeta} for this method's contract.
     */
    public ItemMeta setItemMeta(int meta) {
        this.meta = meta;

        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + item.hashCode();
        result = prime * result + meta;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ItemMeta other = (ItemMeta) obj;
        if (!item.equals(other.item)) return false;
        if (meta != other.meta) return false;
        return true;
    }

    @Override
    public String toString() {
        return "ItemMeta [item=" + item + ", meta=" + meta + "]";
    }
}
