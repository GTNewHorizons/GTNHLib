package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockState_EBSExt;

@Mixin(ExtendedBlockStorage.class)
public class MixinEBS_BlockStates implements BlockState_EBSExt {

    @Unique
    private BlockStateStorage gtnhlib$blockStates;

    @Override
    public BlockStateStorage gtnhlib$getBlockStateStorage() {
        return gtnhlib$blockStates;
    }

    @Override
    public void gtnhlib$setBlockStateStorage(BlockStateStorage storage) {
        gtnhlib$blockStates = storage;
    }
}
