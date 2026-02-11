package com.gtnewhorizon.gtnhlib.mixins.late;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
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
    @Unique
    private boolean gTNHLib$enhancedInfusion;

    // Get the recipe from the crafting method
    @Inject(
            method = "craftingStart",
            at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;recipeType:I"))
    public void setRecipe(CallbackInfo ci, @Local InfusionRecipe recipe) {
        this.gTNHLib$recipe = recipe;
        this.gTNHLib$enhancedInfusion = this.gTNHLib$recipe instanceof EnhancedInfusionRecipe;
    }

    // if recipe is instanceOf EnhancedInfusionRecipe, replace item with the corresponding replacement item, if defined
    @Inject(
            method = "craftCycle",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lthaumcraft/common/tiles/TilePedestal;getStackInSlot", ordinal = 4))
    public void itemReplacement(CallbackInfo ci, @Local boolean valid, @Local TileEntity te, @Local int slot) {
        if (this.gTNHLib$recipe instanceof EnhancedInfusionRecipe eir || this.gTNHLib$enhancedInfusion) {
            TilePedestal pedestal = (TilePedestal) te;
            ItemStack stack = pedestal.getStackInSlot(0);
            ItemStack replacement = eir.getReplacement(stack);
            this.recipeIngredients.remove(slot);
            pedestal.setInventorySlotContents(0, replacement);
            ci.cancel();

        }
    }

    @Inject(method = "craftingFinish", at = @At(value = "RETURN"))
    public void clearRecipe(CallbackInfo ci) {
        this.gTNHLib$recipe = null;
        this.gTNHLib$enhancedInfusion = false;
    }

    // write to NBT with the other ingredients and stuff
    @Inject(method = "writeToNBT", at = @At(value = "INVOKE",
        target = "Lthaumcraft/api/TileThaumcraft;writeToNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", shift = At.Shift.AFTER))
    public void writeToNBT(CallbackInfo ci, @Local NBTTagCompound nbtCompound) {
        if (this.gTNHLib$enhancedInfusion && !((EnhancedInfusionRecipe)this.gTNHLib$recipe).isRepEmpty()) {
            NBTTagList nbttaglist = new NBTTagList();
            for(EnhancedInfusionRecipe.ReplacementMap triad : ((EnhancedInfusionRecipe)this.gTNHLib$recipe).getReplacementMap()) {
                    NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                    triad.input().writeToNBT(nbttagcompound1);
                    triad.output().writeToNBT(nbttagcompound1);
                    nbttagcompound1.setBoolean("strict", triad.strict());
                    nbttaglist.appendTag(nbttagcompound1);
            }
            nbtCompound.setBoolean("isEnhancedRecipe", this.gTNHLib$enhancedInfusion);
            nbtCompound.setTag("replacments", nbttaglist);
        }
    }

}
