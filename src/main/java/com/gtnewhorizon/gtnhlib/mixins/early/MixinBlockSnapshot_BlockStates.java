package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.BlockSnapshot;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockSnapshot_Ext;
import com.gtnewhorizon.gtnhlib.blockstate.storage.NativeBlockStateAware;
import com.gtnewhorizon.gtnhlib.blockstate.storage.BlockStateStorage;
import com.gtnewhorizon.gtnhlib.util.EBSUtil;

@Mixin(value = BlockSnapshot.class, remap = false)
public class MixinBlockSnapshot_BlockStates implements BlockSnapshot_Ext {

    @Unique
    private @Nullable BlockState gtnhlib$capturedState;

    @Override
    public @Nullable BlockState gtnhlib$getCapturedState() {
        return gtnhlib$capturedState;
    }

    @Override
    public void gtnhlib$setCapturedState(@Nullable BlockState state) {
        gtnhlib$capturedState = state;
    }

    /// Captures the extended block state at snapshot creation time.
    /// Constructor (World, int, int, int, Block, int, int) delegates here via this(...), so this covers all factory paths.
    @Inject(method = "<init>(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;I)V", at = @At("TAIL"))
    private void gtnhlib$captureBlockState(World world, int x, int y, int z, Block block, int meta, CallbackInfo ci) {
        if (!(block instanceof NativeBlockStateAware)) return;
        ExtendedBlockStorage ebs = EBSUtil.getEBS(world, x >> 4, y >> 4, z >> 4);
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, false);
        if (storage == null) return;
        gtnhlib$capturedState = storage.getBlockState(x & 0xF, y & 0xF, z & 0xF, null);
    }

    /// Writes the captured state back after restore(). The chunk mixin will have written the default state during
    /// the setBlock call inside restore(); this overwrites it with the original.
    @Inject(method = "restore(ZZ)Z", at = @At("TAIL"))
    private void gtnhlib$restoreBlockState(boolean force, boolean applyPhysics, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || gtnhlib$capturedState == null) return;
        BlockSnapshot self = (BlockSnapshot) (Object) this;
        ExtendedBlockStorage ebs = EBSUtil.getEBS(self.world, self.x >> 4, self.y >> 4, self.z >> 4);
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, true);
        if (storage != null) {
            storage.setBlockState(self.x & 0xF, self.y & 0xF, self.z & 0xF, gtnhlib$capturedState);
        }
    }

    @Inject(method = "restoreToLocation(Lnet/minecraft/world/World;IIIZZ)Z", at = @At("TAIL"))
    private void gtnhlib$restoreBlockStateToLocation(World world, int x, int y, int z, boolean force, boolean applyPhysics, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || gtnhlib$capturedState == null) return;
        ExtendedBlockStorage ebs = EBSUtil.getEBS(world, x >> 4, y >> 4, z >> 4);
        BlockStateStorage storage = BlockStateStorage.getStorage(ebs, true);
        if (storage != null) {
            storage.setBlockState(x & 0xF, y & 0xF, z & 0xF, gtnhlib$capturedState);
        }
    }
}
