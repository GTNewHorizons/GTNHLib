package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockState_IBAExt;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;
import com.gtnewhorizon.gtnhlib.util.EBSUtil;

@Mixin(World.class)
public class MixinWorld_BlockStates implements BlockState_IBAExt {

    @Override
    public @Nullable BlockStateStorage gtnhlib$getBlockStateStorage(int x, int y, int z) {
        return BlockStateStorage.getStorage(
            EBSUtil.getEBS((World) (Object) this, x >> 4, y >> 4, z >> 4), false);
    }
}
