package com.gtnewhorizon.gtnhlib.client.model.color;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

/**
 * Utility class for managing block and item color handlers.
 * <p>
 * This allows modders to register {@link IBlockColor} handlers for blocks and retrieve color values for rendering, both
 * in-world and in inventory.
 */
public class BlockColor {

    private static final Map<Block, IBlockColor> HANDLERS = new HashMap<>();

    /**
     * Registers a color handler for one or more blocks.
     *
     * @param handler The IBlockColor handler.
     * @param blocks  The blocks to register the handler for.
     */
    public static void registerBlockColors(IBlockColor handler, Block... blocks) {
        for (Block b : blocks) {
            HANDLERS.put(b, handler);
        }
    }

    public IBlockColor getBlockColor(Block block) {
        return HANDLERS.get(block);
    }

    public static int getColor(Block block, IBlockAccess world, int x, int y, int z, int meta, int tintIndex) {
        return getColor(block, world, null, x, y, z, meta, tintIndex);
    }

    public static int getColor(Block block, ItemStack stack, int tintIndex) {
        return getColor(block, null, stack, 0, 0, 0, stack.getItemDamage(), tintIndex);
    }

    /**
     * Internal method that fetches the color of a block or item stack, using a registered {@link IBlockColor} if
     * present.
     * <p>
     * Fallback order:
     * <ul>
     * <li>If handler exists and returns != -1, use that value.</li>
     * <li>If item stack exists, call stack.getItem().getColorFromItemStack()</li>
     * <li>If world exists, call block.colorMultiplier(world, x, y, z)</li>
     * <li>Else return white (0xFFFFFF)</li>
     * </ul>
     *
     * @param block     The block.
     * @param world     The world, may be null if rendering item.
     * @param stack     The item stack, may be null if rendering block.
     * @param x         X position (for block color lookup).
     * @param y         Y position.
     * @param z         Z position.
     * @param tintIndex Tint index.
     * @return The final color multiplier as 0xRRGGBB.
     */

    public static int getColor(Block block, @Nullable IBlockAccess world, @Nullable ItemStack stack, int x, int y,
            int z, int meta, int tintIndex) {

        int color = -1;

    // 1. Check for registered handler first, fallback to block's own IBlockColor implementation
    IBlockColor handler = HANDLERS.get(block);
    if (handler == null && block instanceof IBlockColor) {
        handler = (IBlockColor) block;
    }

    if (handler != null) {
        color = applyHandler(handler, world, stack, x, y, z, tintIndex);
        if (color != -1) {
            return color;
        }
    }

        // 3. ItemStack color
        if (stack != null && stack.getItem() != null) {
            return stack.getItem().getColorFromItemStack(stack, tintIndex);
        }

        // 4. World color
        if (world != null) {
            return block.colorMultiplier(world, x, y, z);
        }

        // 5. Meta color fallback
        return block.getRenderColor(meta);
    }

    /**
     * Helper method to invoke the handler with correct parameters.
     */
    private static int applyHandler(@Nullable IBlockColor handler, @Nullable IBlockAccess world,
            @Nullable ItemStack stack, int x, int y, int z, int tintIndex) {

        if (handler == null) return -1;

        if (stack != null) {
            return handler.colorMultiplier(stack, tintIndex);
        }

        if (world != null) {
            return handler.colorMultiplier(world, x, y, z, tintIndex);
        }

        return -1;
    }
}
