package com.gtnewhorizon.gtnhlib.mixins.early.block_sounds;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.BlockWithCustomSound;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Shadow
    private Minecraft mc;

    @WrapOperation(
            method = "playAuxSFX",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/block/Block;stepSound:Lnet/minecraft/block/Block$SoundType;"))
    public SoundType redirectStepSound(Block instance, Operation<SoundType> original,
            @Local(argsOnly = true, ordinal = 1) int x, @Local(argsOnly = true, ordinal = 2) int y,
            @Local(argsOnly = true, ordinal = 3) int z) {
        if (instance instanceof BlockWithCustomSound sound) {
            return sound.getSound(this.mc.thePlayer.worldObj, x, y, z);
        }

        return original.call(instance);
    }
}
