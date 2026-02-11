package com.gtnewhorizon.gtnhlib.mixins.late;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.api.thaumcraft.EnhancedInfusionRecipe;
import com.llamalad7.mixinextras.sugar.Local;

import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TilePedestal;

@Mixin(value = TileInfusionMatrix.class, remap = false)
public abstract class MixinEnhancedInfusionRecipe extends TileThaumcraft {

    @Shadow
    private ArrayList<ItemStack> recipeIngredients;
    @Unique
    private InfusionRecipe gTNHLib$recipe;

    // Get the recipe from the crafting method
    @Inject(
            method = "craftingStart",
            at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;recipeType:I"))
    public void setRecipe(CallbackInfo ci, @Local InfusionRecipe recipe) {
        this.gTNHLib$recipe = recipe;
    }

    // if recipe is instanceOf EnhancedInfusionRecipe, replace item with the corresponding replacement item, if defined
    @Inject(
            method = "craftCycle",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lthaumcraft/common/tiles/TilePedestal;getStackInSlot", ordinal = 4))
    public void itemReplacement(CallbackInfo ci, @Local boolean valid, @Local TileEntity te, @Local int slot) {
        if (this.gTNHLib$recipe instanceof EnhancedInfusionRecipe eir) {
            TilePedestal pedestal = (TilePedestal) te;
            ItemStack stack = pedestal.getStackInSlot(0);
            ItemStack replacement = eir.getReplacement(stack);
            if (replacement != null) {
                this.recipeIngredients.remove(slot);
                pedestal.setInventorySlotContents(0, replacement);
                ci.cancel();
            }
        }
    }
}
