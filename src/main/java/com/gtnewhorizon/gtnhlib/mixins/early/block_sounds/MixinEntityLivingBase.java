package com.gtnewhorizon.gtnhlib.mixins.early.block_sounds;

import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.BlockWithCustomSound;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @WrapOperation(
            method = "fall",
            require = 1,
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/block/Block;stepSound:Lnet/minecraft/block/Block$SoundType;"))
    public SoundType redirectStepSound(Block instance, Operation<SoundType> original) {
        if (instance instanceof BlockWithCustomSound sound) {
            Entity self = (Entity) (Object) this;

            int x = MathHelper.floor_double(self.posX);
            int y = MathHelper.floor_double(self.posY - 0.20000000298023224D - (double) self.yOffset);
            int z = MathHelper.floor_double(self.posZ);

            // Shadow doesn't work for fields on superclasses for some reason :caught:
            return sound.getSound(self.worldObj, x, y, z);
        }

        return original.call(instance);
    }
}
