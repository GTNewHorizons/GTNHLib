package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemCloth;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.ITranslucentItem;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(RenderItem.class)
public class MixinRenderItem_Translucency {

    @Definition(id = "getItem", method = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;")
    @Definition(id = "itemstack", local = @Local(type = ItemStack.class))
    @Definition(id = "ItemCloth", type = ItemCloth.class)
    @Expression("itemstack.getItem() instanceof ItemCloth")
    @WrapOperation(
            method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V",
            at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private boolean gtnhlib$isItemTranslucent(Object object, Operation<Boolean> original) {
        return object instanceof ITranslucentItem || original.call(object);
    }
}
