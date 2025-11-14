package com.gtnewhorizon.gtnhlib.mixins.early.block_sounds;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.item.ItemBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.IBlockWithCustomSound;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin({ ItemBlock.class })
public class MixinItemBlock {

    @WrapOperation(
            method = "onItemUse",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/block/Block;stepSound:Lnet/minecraft/block/Block$SoundType;"))
    public SoundType redirectStepSound(Block instance, Operation<SoundType> original,
            @Local(argsOnly = true) World world, @Local(argsOnly = true, ordinal = 0) int x,
            @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
        if (instance instanceof IBlockWithCustomSound sound) {
            return sound.getSound(world, x, y, z);
        }

        return original.call(instance);
    }
}
