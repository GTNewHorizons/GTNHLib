package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;

@Mixin(Block.class)
public class MixinBlockParticle {

    @Inject(method = "addDestroyEffects", at = @At("HEAD"), cancellable = true, remap = false)
    public void gtnhlib$addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer,
            CallbackInfoReturnable<Boolean> cir) {
        if (((Block) (Object) this).getRenderType() == ModelISBRH.JSON_ISBRH_ID) {
            cir.setReturnValue(ModelISBRH.INSTANCE.addDestroyEffects(world, x, y, z, meta, effectRenderer));
        }
    }

    @Inject(method = "addHitEffects", at = @At("HEAD"), cancellable = true, remap = false)
    public void gtnhlib$addHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer,
            CallbackInfoReturnable<Boolean> cir) {
        if (((Block) (Object) this).getRenderType() == ModelISBRH.JSON_ISBRH_ID) {
            cir.setReturnValue(ModelISBRH.INSTANCE.addHitEffects(worldObj, target, effectRenderer));
        }
    }

}
