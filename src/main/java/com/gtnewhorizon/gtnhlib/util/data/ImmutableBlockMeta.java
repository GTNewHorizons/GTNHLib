package com.gtnewhorizon.gtnhlib.util.data;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;

/**
 * An immutable block-meta pair. This must not be cast down to its mutable version unless you have a very good reason.
 * It can be assumed that the values of {@link #getBlock()} and {@link #getBlockMeta()} will never change for this
 * object if the object is exposed through an API.
 */
public interface ImmutableBlockMeta {

    /**
     * The value of this must not change while this object is exposed via an API.
     * 
     * @return The block stored in this pair.
     */
    @Nonnull
    public Block getBlock();

    /**
     * The value of this must not change while this object is exposed via an API.
     * 
     * @return The block's metadata stored in this pair.
     */
    public int getBlockMeta();

    /**
     * Gets the corresponding item for this block. Subclasses may provide a faster implementation.
     */
    public default Item getItem() {
        return Item.getItemFromBlock(getBlock());
    }

    /**
     * Checks if this pair matches the given block & meta.
     * 
     * @param block The block.
     * @param meta  The meta. If this parameter or {@link #getBlockMeta()} equals {@link OreDictionary.WILDCARD_VALUE}
     *              then meta checks are ignored.
     * @return Whether this pair matches or not.
     */
    public default boolean matches(Block block, int meta) {
        return getBlock() == block
                && (meta == OreDictionary.WILDCARD_VALUE || getBlockMeta() == OreDictionary.WILDCARD_VALUE
                        || getBlockMeta() == meta);
    }
}
