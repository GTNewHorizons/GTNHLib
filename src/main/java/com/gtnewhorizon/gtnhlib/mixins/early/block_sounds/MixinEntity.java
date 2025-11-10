package com.gtnewhorizon.gtnhlib.mixins.early.block_sounds;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.BlockWithCustomSound;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin({ Entity.class, EntityHorse.class })
public class MixinEntity {

    @WrapOperation(
            method = "func_145780_a",
            at = @At(
                    value = "FIELD",
                    ordinal = 0,
                    target = "Lnet/minecraft/block/Block;stepSound:Lnet/minecraft/block/Block$SoundType;"))
    public SoundType redirectStepSound(Block instance, Operation<SoundType> original,
            @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y,
            @Local(argsOnly = true, ordinal = 2) int z) {
        if (instance instanceof BlockWithCustomSound sound) {
            return sound.getSound(((Entity) (Object) this).worldObj, x, y, z);
        }

        return original.call(instance);
    }
}
