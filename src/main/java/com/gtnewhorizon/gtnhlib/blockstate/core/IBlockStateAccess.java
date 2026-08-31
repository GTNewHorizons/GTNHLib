package com.gtnewhorizon.gtnhlib.blockstate.core;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/// Implemented on something that extends [IBlockAccess] (typically [World]s, but may be other world-like objects).
public interface IBlockStateAccess extends IBlockAccess {

    /// Gets the native state from a given voxel.
    @Nullable
    BlockState getBlockState(int x, int y, int z);
}
