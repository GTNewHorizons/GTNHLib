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
     * <p>
     * Example usage:
     *
     * <pre>
     * BlockColor.registerBlockColors(new IBlockColor() {
     *
     *     &#64;Override
     *     public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
     *         // Return red for main layer, green for secondary
     *         return tintIndex == 0 ? 0xFF0000 : 0x00FF00;
     *     }
     *
     *     &#64;Override
     *     public int colorMultiplier(ItemStack stack, int tintIndex) {
     *         // Return blue for main layer, yellow for secondary
     *         return tintIndex == 0 ? 0x0000FF : 0xFFFF00;
     *     }
     * }, ModBlocks.MY_CUSTOM_BLOCK);
     * </pre>
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

    /**
     * Gets the color of a block in the world at a given position.
     *
     * @param block     The block.
     * @param world     The world access.
     * @param x         X coordinate.
     * @param y         Y coordinate.
     * @param z         Z coordinate.
     * @param tintIndex Tint index of the block texture.
     * @return The color multiplier as 0xRRGGBB.
     */
    public static int getColor(Block block, IBlockAccess world, int x, int y, int z, int tintIndex) {
        return getColor(block, world, null, x, y, z, tintIndex);
    }

    /**
     * Gets the color of an item stack.
     *
     * @param block     The block corresponding to the item.
     * @param stack     The item stack.
     * @param tintIndex Tint index of the item texture.
     * @return The color multiplier as 0xRRGGBB.
     */
    public static int getColor(Block block, ItemStack stack, int tintIndex) {
        return getColor(block, null, stack, 0, 0, 0, tintIndex);
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
            int z, int tintIndex) {
        int color = -1;

        // block implements IBlockColor
        if (block instanceof IBlockColor blockColor) {
            if (stack != null) {
                color = blockColor.colorMultiplier(stack, tintIndex);
            } else if (world != null) {
                color = blockColor.colorMultiplier(world, x, y, z, tintIndex);
            }
        }

        // Use Register
        if (color == -1) {
            IBlockColor handler = HANDLERS.get(block);
            if (handler != null) {
                if (stack != null) {
                    color = handler.colorMultiplier(stack, tintIndex);
                } else if (world != null) {
                    color = handler.colorMultiplier(world, x, y, z, tintIndex);
                }
            }
        }

        // Use flask back
        if (color == -1) {
            if (stack != null && stack.getItem() != null) {
                color = stack.getItem().getColorFromItemStack(stack, tintIndex);
            } else if (world != null) {
                color = block.colorMultiplier(world, x, y, z);
            } else {
                color = 0xFFFFFF;
            }
        }

        return color;
    }
}
