package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.loading.BlockModelInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(EffectRenderer.class)
public class MixinEffectRenderer_FixParticleIcons {

    @Shadow
    protected World worldObj;

    @ModifyExpressionValue(
            method = "addBlockDestroyEffects",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/World;DDDDDDLnet/minecraft/block/Block;I)Lnet/minecraft/client/particle/EntityDiggingFX;"))
    private EntityDiggingFX nhlib$fixBreakIcon(EntityDiggingFX original, @Local(argsOnly = true) Block block,
            @Local(ordinal = 0, argsOnly = true) int x, @Local(ordinal = 1, argsOnly = true) int y,
            @Local(ordinal = 2, argsOnly = true) int z) {
        if (((BlockModelInfo) block).nhlib$isModeled())
            original.setParticleIcon(ModelISBRH.INSTANCE.getParticleIcon(worldObj, x, y, z));
        return original;
    }

    @ModifyExpressionValue(
            method = "addBlockHitEffects",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/World;DDDDDDLnet/minecraft/block/Block;I)Lnet/minecraft/client/particle/EntityDiggingFX;"))
    private EntityDiggingFX nhlib$fixHitIcon(EntityDiggingFX original, @Local(ordinal = 0) Block block,
            @Local(ordinal = 0, argsOnly = true) int x, @Local(ordinal = 1, argsOnly = true) int y,
            @Local(ordinal = 2, argsOnly = true) int z) {
        if (((BlockModelInfo) block).nhlib$isModeled())
            original.setParticleIcon(ModelISBRH.INSTANCE.getParticleIcon(worldObj, x, y, z));
        return original;
    }
}
