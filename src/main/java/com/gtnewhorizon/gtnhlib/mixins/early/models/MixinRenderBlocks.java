package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {

    @Shadow
    public IBlockAccess blockAccess;

    @Inject(
            method = "renderBlockByRenderType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBlocks;setRenderBoundsFromBlock(Lnet/minecraft/block/Block;)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void nhlib$injectModelISBRH(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir,
            @Local(name = "l") int renderType) {
        if (((BlockModelInfo) block).nhlib$isModeled()) {
            cir.setReturnValue(
                    ModelISBRH.INSTANCE
                            .renderWorldBlock(blockAccess, x, y, z, block, renderType, (RenderBlocks) (Object) this));
        }
    }
}
