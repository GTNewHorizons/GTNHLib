package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemCloth;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.api.ITranslucentItem;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer_Translucency {

    @Definition(id = "getItem", method = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;")
    @Definition(id = "itemstack", local = @Local(type = ItemStack.class))
    @Definition(id = "ItemCloth", type = ItemCloth.class)
    @Expression("itemstack.getItem() instanceof ItemCloth")
    @WrapOperation(method = "renderItemInFirstPerson", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private boolean gtnhlib$isItemTranslucent(Object object, Operation<Boolean> original) {
        return object instanceof ITranslucentItem || original.call(object);
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/MinecraftForgeClient;getItemRenderer(Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;)Lnet/minecraftforge/client/IItemRenderer;"),
            remap = false)
    private void gtnhlib$isItemTranslucent(EntityLivingBase p_78443_1_, ItemStack p_78443_2_, int p_78443_3_,
            IItemRenderer.ItemRenderType type, CallbackInfo ci) {
        if (p_78443_2_.getItem() instanceof ITranslucentItem) {
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        }
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V",
                    shift = At.Shift.BY,
                    by = 3,
                    ordinal = 3),
            remap = false)
    private void gtnhlib$isItemTranslucent2(EntityLivingBase p_78443_1_, ItemStack p_78443_2_, int p_78443_3_,
            IItemRenderer.ItemRenderType type, CallbackInfo ci) {
        if (p_78443_2_.getItem() instanceof ITranslucentItem) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
