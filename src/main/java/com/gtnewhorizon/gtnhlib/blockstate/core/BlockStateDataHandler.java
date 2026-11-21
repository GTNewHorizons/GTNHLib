package com.gtnewhorizon.gtnhlib.blockstate.core;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockStateDataRegistry;

/**
 * Handles data for a block whos state can not be represented by only block and meta., IE fence connections or a double
 * plant's variant in its lower half. It replicates what getActualState does on later MC versions.
 *
 * <p>
 * The idea is to present each unique visual state by a unique string.
 *
 * <p>
 * {@link #encode} checks the world around the block and generates a string, {@link #decode} turns that string back into
 * blocks around the block, so it renders the same way without the real world (IE in LODs or previews). Each distinct
 * value is its own state with its own model, so the data should only contain what changes the block's look.
 *
 * <p>
 * Registered via {@link BlockStateDataRegistry}.
 */
public interface BlockStateDataHandler {

    enum Type {
        /**
         * The block's own state that's kept outside its meta, IE a double plant's variant in its lower half or flwoer
         * pots. It only changes together with the block (or where it's kept), so it can be encoded once from the real
         * world and saved with the block.
         */
        LOCAL_ONLY,
        /**
         * Depends on the blocks around it, IE fence connections. It changes whenever one of them changes, so it
         * shouldn't be saved but encoded again from whatever blocks are known at that time.
         */
        NEIGHBOR_DEPENDENT,
    }

    Type getType();

    /**
     * Called on any thread, so this must be thread safe. The world may not be a real {@link net.minecraft.world.World}:
     * it may have no tile entities, and blocks that aren't known are air.
     *
     * @return the block's data, null or an empty string for none. Formatted like vanilla's block state variants:
     *         {@code name=value} pairs separated by {@code ,}, names and values only use {@code a-z 0-9 _}, IE
     *         {@code east=false,north=true}. Invalid data is ignored. Pairs should be sorted by name, so equal states
     *         give equal strings.
     */
    @Nullable
    String encode(IBlockAccess world, int x, int y, int z, Block block, int meta);

    /**
     * Sets up the blocks around the block so it renders the way {@link #encode} saw it (IE copies of the fence where it
     * connects). Called on any thread, so this must be thread safe.
     *
     * @param data  the value {@link #encode} returned, never null or empty
     * @param world only contains the block itself until this adds to it, only valid during this call
     */
    void decode(Block block, int meta, String data, BlockStateDataWorld world);

    /**
     * Sets up the blocks around the block when it has no data, IE because it was never encoded. Only needed if the
     * block's renderer draws nothing without some of them (IE a double plant's upper half without its lower half).
     * Called on any thread, so this must be thread safe.
     *
     * @param world only contains the block itself until this adds to it, only valid during this call
     */
    default void decodeDefault(Block block, int meta, BlockStateDataWorld world) {}
}
