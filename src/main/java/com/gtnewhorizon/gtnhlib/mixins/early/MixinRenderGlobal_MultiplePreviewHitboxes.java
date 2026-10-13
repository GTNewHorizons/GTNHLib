package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.IBlockWithMultiplePreviewAABBs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal_MultiplePreviewHitboxes {

    @Shadow
    public static void drawOutlinedBoundingBox(AxisAlignedBB p_147590_0_, int p_147590_1_) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private WorldClient theWorld;

    @WrapOperation(
            method = "drawSelectionBox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;drawOutlinedBoundingBox(Lnet/minecraft/util/AxisAlignedBB;I)V"))
    private void gtnhlib$drawMultiHitboxes(AxisAlignedBB p_147590_0_, int p_147590_1_, Operation<Void> original,
            @Local(name = "block") Block block, @Local(argsOnly = true, name = "p_72731_2_") MovingObjectPosition hit,
            @Local(name = "d0") double playerX, @Local(name = "d1") double playerY,
            @Local(name = "d2") double playerZ) {
        if (block instanceof IBlockWithMultiplePreviewAABBs aabbs) {
            var bbs = aabbs.getPreviewAABBs(this.theWorld, hit.blockX, hit.blockY, hit.blockZ);

            if (bbs != null) {
                for (var bb : bbs) {
                    drawOutlinedBoundingBox(
                            bb.expand(0.002F, 0.002F, 0.002F).getOffsetBoundingBox(-playerX, -playerY, -playerZ),
                            -1);
                }

                return;
            }
        }

        original.call(p_147590_0_, p_147590_1_);
    }
}
