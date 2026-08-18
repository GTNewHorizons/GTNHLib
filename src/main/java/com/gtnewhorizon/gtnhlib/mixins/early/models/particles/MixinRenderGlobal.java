package com.gtnewhorizon.gtnhlib.mixins.early.models.particles;

import static com.gtnewhorizon.gtnhlib.api.BlockModelInfo.isModeled;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @SuppressWarnings("NameDoesntMatchTargetClass")
    @WrapOperation(
            method = "playAuxSFX",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;spawnParticle(Ljava/lang/String;DDDDDD)V",
                    ordinal = 2))
    private void nhlib$fixModeledFallParticles(WorldClient wc, String particleName, double x, double y, double z,
            double vX, double vY, double vZ, Operation<Void> original, @Local(name = "block") Block block,
            @Local(name = "p_72706_3_", argsOnly = true) int blockX,
            @Local(name = "p_72706_4_", argsOnly = true) int blockY,
            @Local(name = "p_72706_5_", argsOnly = true) int blockZ) {
        if (!isModeled(block)) {
            original.call(wc, particleName, x, y, z, vX, vY, vZ);
            return;
        }

        // This is a modeled block! Refer to our helper for particles instead of the default.
        ModelISBRH.spawnParticleCommon(wc, blockX, blockY, blockZ, x, y, z, vX, vY, vZ);
    }
}
