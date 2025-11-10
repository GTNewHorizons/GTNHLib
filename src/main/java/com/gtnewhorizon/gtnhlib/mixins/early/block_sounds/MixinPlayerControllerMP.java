package com.gtnewhorizon.gtnhlib.mixins.early.block_sounds;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.BlockWithCustomSound;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Shadow
    @Final
    private Minecraft mc;

    @WrapOperation(
            method = "onPlayerDamageBlock",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/block/Block;stepSound:Lnet/minecraft/block/Block$SoundType;"))
    public SoundType redirectStepSound(Block instance, Operation<SoundType> original,
            @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y,
            @Local(argsOnly = true, ordinal = 2) int z) {
        if (instance instanceof BlockWithCustomSound sound) {
            return sound.getSound(this.mc.thePlayer.worldObj, x, y, z);
        }

        return original.call(instance);
    }
}
