package com.gtnewhorizon.gtnhlib.blockstate.storage;

import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.jetbrains.annotations.Nullable;

import com.falsepattern.chunk.api.DataRegistry;
import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStatePool;
import com.gtnewhorizon.gtnhlib.util.EBSUtil;
import cpw.mods.fml.common.Loader;
import lombok.Getter;

public class NativeBlockStates {

    @Getter
    private static boolean isEnabled;

    public static void init() {
        isEnabled = GTNHLibConfig.enableNativeBlockStates && Loader.isModLoaded("chunkapi");

        if (isEnabled) {
            DataRegistry.registerDataManager(new BSSDataManager(), 10);
        }
    }

    public static boolean setBlockState(World world, int x, int y, int z, @Nullable BlockState state, int flags) {
        if (!isEnabled) {
            throw new IllegalStateException("Native BlockState storage is not enabled");
        }

        if (state == null) {
            if (!world.setBlockToAir(x, y, z)) return false;
        } else {
            if (!(state.getBlock() instanceof NativeBlockStateAware bsa)) return false;

            world.setBlock(x, y, z, bsa.getBlockForState(state), bsa.getMetaForState(state), flags);

            ExtendedBlockStorage ebs = EBSUtil.getEBS(world, x >> 4, y >> 4, z >> 4);
            BlockStateStorage storage = BlockStateStorage.getStorage(ebs, true);

            storage.setBlockState(x & 0xF, y & 0xF, z & 0xF, state);
            world.markBlockForUpdate(x, y, z);
        }

        return true;
    }

    public static BlockState getBlockState(BlockStatePool pool, World world, int x, int y, int z) {
        if (!isEnabled) {
            throw new IllegalStateException("Native BlockState storage is not enabled");
        }

        ExtendedBlockStorage ebs = EBSUtil.getEBS(world, x >> 4, y >> 4, z >> 4);
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, false);

        if (storage == null) return null;

        return storage.getBlockState(x & 0xF, y & 0xF, z & 0xF, pool);
    }
}
