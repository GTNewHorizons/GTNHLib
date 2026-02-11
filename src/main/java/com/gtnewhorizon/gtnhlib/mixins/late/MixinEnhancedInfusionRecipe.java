package com.gtnewhorizon.gtnhlib.mixins.late;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import net.minecraftforge.oredict.OreDictionary;
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
    private boolean gTNHLib$enhancedInfusion = false;
    @Unique
    private List<EnhancedInfusionRecipe.ReplacementMap> gTNHLib$replacements = null;

    @Unique
    public ItemStack getReplacement(ItemStack key) {
        for (EnhancedInfusionRecipe.ReplacementMap replacement : this.gTNHLib$replacements) {
            if (OreDictionary.itemMatches(replacement.input(), key, replacement.strict())) {
                return replacement.output();
            }
        }
        return null;
    }

    // Get the recipe from the crafting method
    @Inject(
            method = "craftingStart",
            at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;recipeType:I"))
    public void setRecipe(CallbackInfo ci, @Local InfusionRecipe recipe) {
        this.gTNHLib$enhancedInfusion = (recipe instanceof EnhancedInfusionRecipe);
        if (this.gTNHLib$enhancedInfusion)
        this.gTNHLib$replacements = ((EnhancedInfusionRecipe) recipe).getReplacementMap();
    }

    // if recipe is instanceOf EnhancedInfusionRecipe, replace item with the corresponding replacement item, if defined
    @Inject(
            method = "craftCycle",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lthaumcraft/common/tiles/TilePedestal;getStackInSlot(I)Lnet/minecraft/item/ItemStack;", ordinal = 4))
    public void itemReplacement(CallbackInfo ci, @Local boolean valid, @Local TileEntity te, @Local int slot) {
        if (this.gTNHLib$enhancedInfusion) {
            TilePedestal pedestal = (TilePedestal) te;
            ItemStack stack = pedestal.getStackInSlot(0);
            ItemStack replacement = getReplacement(stack);
            this.recipeIngredients.remove(slot);
            pedestal.setInventorySlotContents(0, replacement);
            ci.cancel();
        }
    }

    @Inject(method = "craftingFinish", at = @At(value = "RETURN"))
    public void clearRecipe(CallbackInfo ci) {
        this.gTNHLib$replacements = null;
        this.gTNHLib$enhancedInfusion = false;
    }

    // write to NBT with the other ingredients and stuff
    @Inject(method = "writeToNBT", at = @At(value = "INVOKE",
        target = "Lthaumcraft/api/TileThaumcraft;writeToNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", shift = At.Shift.AFTER))
    public void writeToNBT(CallbackInfo ci, @Local NBTTagCompound nbtCompound) {
        if (this.gTNHLib$enhancedInfusion) {
            NBTTagList nbttaglist = new NBTTagList();
            for(EnhancedInfusionRecipe.ReplacementMap triad : this.gTNHLib$replacements) {
                NBTTagCompound replacementMap = new NBTTagCompound();
                    if (triad.input() == null)
                            continue;
                    replacementMap.setTag("input", triad.input().writeToNBT(new NBTTagCompound()));

                    if (triad.output() != null)
                        replacementMap.setTag("output", triad.output().writeToNBT(new NBTTagCompound()));
                    else
                        replacementMap.setBoolean("nullOutput", true);

                    replacementMap.setBoolean("strict", triad.strict());
                    nbttaglist.appendTag(replacementMap);
            }
            nbtCompound.setBoolean("isEnhancedRecipe", this.gTNHLib$enhancedInfusion);
            nbtCompound.setTag("replacments", nbttaglist);
        }
    }

    // write to NBT with the other ingredients and stuff
    @Inject(method = "readFromNBT", at = @At(value = "INVOKE",
        target = "Lthaumcraft/api/TileThaumcraft;readFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", shift = At.Shift.AFTER))
    public void readFromNBT(CallbackInfo ci, @Local NBTTagCompound nbtCompound) {
        this.gTNHLib$enhancedInfusion = nbtCompound.getBoolean("isEnhancedRecipe");

        if (this.gTNHLib$enhancedInfusion) {
            NBTTagList nbttaglist = nbtCompound.getTagList("replacments", 9); // the list of replacements
            this.gTNHLib$replacements = new ArrayList<EnhancedInfusionRecipe.ReplacementMap>(); //make a new map
            for(int i = 0; i < nbttaglist.tagCount(); ++i) {  //for every replacement entry in replacements
                NBTTagCompound replacementEntry = nbttaglist.getCompoundTagAt(i); //get the replacament entry
                ItemStack input = ItemStack.loadItemStackFromNBT(replacementEntry.getCompoundTag("input"));
                ItemStack output = null;
                if (!replacementEntry.getBoolean("nullOutput"))
                    output = ItemStack.loadItemStackFromNBT(replacementEntry.getCompoundTag("output"));

                boolean strict = replacementEntry.getBoolean("strict");

                this.gTNHLib$replacements.add(new EnhancedInfusionRecipe.ReplacementMap(input, output, strict));
            }
        }
    }

}
