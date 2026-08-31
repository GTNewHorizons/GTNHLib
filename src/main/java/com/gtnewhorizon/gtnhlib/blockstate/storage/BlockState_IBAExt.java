package com.gtnewhorizon.gtnhlib.blockstate.storage;

import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/// Implemented on [IBlockAccess] types (World, ChunkCache) to expose [BlockStateStorage] lookups
/// without requiring a cast to [net.minecraft.world.World]. This allows block rendering code
/// (which only receives [IBlockAccess]) to read extended block state.
@Internal
public interface BlockState_IBAExt {

    @Nullable BlockStateStorage gtnhlib$getBlockStateStorage(int x, int y, int z);

    static @Nullable BlockStateStorage get(IBlockAccess iba, int x, int y, int z) {
        if (iba instanceof BlockState_IBAExt ext) {
            return ext.gtnhlib$getBlockStateStorage(x, y, z);
        }
        return null;
    }
}
