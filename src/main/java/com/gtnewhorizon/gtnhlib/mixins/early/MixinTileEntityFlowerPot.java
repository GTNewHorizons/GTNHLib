package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFlowerPot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.IFlowerPottable;

@Mixin(TileEntityFlowerPot.class)
public class MixinTileEntityFlowerPot {

    @Shadow
    private Item flowerPotItem;

    @Inject(method = "writeToNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
    private void hodgepodge$writeFlowerPottableNBT(NBTTagCompound compound, CallbackInfo ci) {
        if (flowerPotItem instanceof IFlowerPottable pottable) {
            pottable.writeToFlowerPotNBT(compound);
        }
    }
}
