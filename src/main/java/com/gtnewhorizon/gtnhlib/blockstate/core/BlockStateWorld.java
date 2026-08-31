package com.gtnewhorizon.gtnhlib.blockstate.core;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.storage.NativeBlockStateAware;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;

/// Implemented on [World]s to expose [BlockState] setters and getters.
public interface BlockStateWorld extends IBlockStateAccess {

    /// Sets the block in a given location to the provided block state.
    /// This updates the [Block] in the voxel to whatever [BlockState#getBlock()] returns, and sets the metadata to
    /// whatever [BlockState#getBlockMeta(int)] returns (with an existing meta of 0, or the previous metadata if the
    /// block was already correct). Block equality can be modified by implementing [NativeBlockStateAware] on the [Block] and
    /// overriding [NativeBlockStateAware#isSameBlock(Block)].
    /// This also stores the [BlockState] into the [BlockStateStorage]
    boolean setBlockState(int x, int y, int z, BlockState state, int flags);

}
