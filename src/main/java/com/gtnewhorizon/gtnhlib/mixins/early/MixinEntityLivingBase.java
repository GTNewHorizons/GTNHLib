package com.gtnewhorizon.gtnhlib.mixins.early;

import com.gtnewhorizon.gtnhlib.client.event.LivingEquipmentChangeEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase {

    @Inject(method = "onUpdate", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/WorldServer;getEntityTracker()Lnet/minecraft/entity/EntityTracker;"))
    private void addEquipmentChangeEventBus(CallbackInfo ci, @Local(ordinal = 0) int slot, @Local (ordinal = 0) ItemStack prevItem, @Local (ordinal = 1) ItemStack newItem) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new LivingEquipmentChangeEvent((EntityLivingBase) (Object) this, slot, prevItem, newItem));
    }
}
