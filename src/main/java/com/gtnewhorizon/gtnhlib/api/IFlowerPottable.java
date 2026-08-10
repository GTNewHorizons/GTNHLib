package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;

/**
 * Allow this block to be placed into vanilla Flower Pots. If your block is simple and all of its standard ItemBlock
 * textures will work in a flower pot, you don't need to override anything.
 */
public interface IFlowerPottable {

    default boolean isFlowerPottable(int meta) {
        return true;
    }

    /**
     * Write additional data to the NBT of the flower pot. This NBT can be accessed in
     * {@link #renderFlowerPot(NBTTagCompound, IBlockAccess, Block, int, int, int, RenderBlocks)} (NBTTagCompound)} and
     * {@link #replaceFlowerPotDrop(NBTTagCompound)}
     */
    default void writeToFlowerPotNBT(NBTTagCompound compound, int meta) {}

    /**
     * Override standard flower pot rendering. Return true if rendering was overriden!
     */
    default boolean renderFlowerPot(NBTTagCompound compound, IBlockAccess blockAccess, Block block, int x, int y, int z,
            RenderBlocks render) {
        return false;
    }

    /**
     * Replace the flower drop from a flower pot containing this block. Will drop the default (a new ItemStack with just
     * item id and meta) if this returns null.
     */
    default ItemStack replaceFlowerPotDrop(NBTTagCompound compound) {
        return null;
    }
}
