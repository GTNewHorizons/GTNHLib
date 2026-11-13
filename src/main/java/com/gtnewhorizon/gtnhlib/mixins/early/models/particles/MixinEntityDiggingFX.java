package com.gtnewhorizon.gtnhlib.mixins.early.models.particles;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntityFX;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;

@Mixin(EntityDiggingFX.class)
public class MixinEntityDiggingFX {

    @Shadow
    private Block field_145784_a;

    @Inject(method = "applyColourMultiplier", at = @At("HEAD"))
    private void nhlib$fixModeledIcon(int x, int y, int z, CallbackInfoReturnable<EntityDiggingFX> cir) {
        if (((BlockModelInfo) this.field_145784_a).nhlib$isModeled()) {
            final EntityFX self = (EntityFX) (Object) this;
            self.setParticleIcon(ModelISBRH.INSTANCE.get().getParticleIcon(self.worldObj, x, y, z));
        }
    }
}
