package com.gtnewhorizon.gtnhlib.mixins.early.models.particles;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@SuppressWarnings("UnusedMixin")
@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    public World worldObj;

    @WrapOperation(
            method = "onEntityUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnParticle(Ljava/lang/String;DDDDDD)V"))
    private void nhlib$fixBreakIcon(World instance, String particleName, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, Operation<Void> original, @Local(name = "block") Block block,
            @Local(name = "j") int j, @Local(name = "i") int i, @Local(name = "k") int k) {
        if (!((BlockModelInfo) block).nhlib$isModeled()) {
            original.call(instance, particleName, x, y, z, velocityX, velocityY, velocityZ);
            return;
        }

        // This is a modeled block! Refer to our helper for particles instead of the default.
        ModelISBRH.spawnParticleCommon(worldObj, j, i, k, x, y, z, velocityX, velocityY, velocityZ);
    }
}
