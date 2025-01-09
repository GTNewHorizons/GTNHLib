package com.gtnewhorizon.gtnhlib.util.data;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * An immutable item-meta pair. This must not be cast down to its mutable version unless you have a very good reason. It
 * can be assumed that the values of {@link #getItem()} and {@link #getItemMeta()} will never change for this object if
 * the object is exposed through an API.
 */
public interface ImmutableItemMeta {

    /**
     * The value of this must not change while this object is exposed via an API.
     * 
     * @return The item stored in this pair.
     */
    @Nonnull
    public Item getItem();

    /**
     * The value of this must not change while this object is exposed via an API.
     * 
     * @return The item's metadata stored in this pair. May be {@link OreDictionary#WILDCARD_VALUE}.
     */
    public int getItemMeta();

    /**
     * Gets the corresponding block for this item. Subclasses may provide a faster implementation.
     */
    public default Block getBlock() {
        return Block.getBlockFromItem(getItem());
    }

    /**
     * Checks if this pair matches the given ItemStack's item and metadata.
     */
    public default boolean matches(ItemStack stack) {
        if (stack == null) return false;

        return matches(stack.getItem(), Items.feather.getDamage(stack));
    }

    /**
     * Checks if this pair matches the given item & meta.
     * 
     * @param Item The item.
     * @param meta The meta. If this parameter or {@link #getItemMeta()} equals {@link OreDictionary#WILDCARD_VALUE}
     *             then meta checks are ignored.
     * @return Whether this pair matches or not.
     */
    public default boolean matches(Item item, int meta) {
        return getItem() == item
                && (meta == OreDictionary.WILDCARD_VALUE || getItemMeta() == OreDictionary.WILDCARD_VALUE
                        || getItemMeta() == meta);
    }

    /** Converts this pair to an ItemStack. */
    public default ItemStack toStack(int amount) {
        int meta = getItemMeta();

        return new ItemStack(getItem(), amount, meta == OreDictionary.WILDCARD_VALUE ? 0 : meta);
    }
}
