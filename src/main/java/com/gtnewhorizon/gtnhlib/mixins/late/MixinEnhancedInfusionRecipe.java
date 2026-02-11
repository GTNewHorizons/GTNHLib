package com.gtnewhorizon.gtnhlib.mixins.late;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
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
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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
    private List<EnhancedInfusionRecipe.ReplacementMap> gTNHLib$replacements = new ArrayList<>();

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
        if (recipe instanceof EnhancedInfusionRecipe r) {
            this.gTNHLib$replacements = r.getReplacementMap();
        }
    }

    // if recipe is instanceOf EnhancedInfusionRecipe, replace item with the corresponding replacement item, if defined
    @Inject(
            method = "craftCycle",
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lthaumcraft/common/tiles/TilePedestal;getStackInSlot(I)Lnet/minecraft/item/ItemStack;",
                    ordinal = 4))
    public void itemReplacement(CallbackInfo ci, @Local boolean valid, @Local TileEntity te, @Local int slot) {
        if (!this.gTNHLib$replacements.isEmpty()) {
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
    }

    // write to NBT with the other ingredients and stuff
    @WrapMethod(method = "writeToNBT")
    public void writeToNBT(NBTTagCompound nbtCompound, Operation<Void> original) {
        original.call(nbtCompound);
        if (!this.gTNHLib$replacements.isEmpty()) {
            NBTTagList nbttaglist = new NBTTagList();
            for (EnhancedInfusionRecipe.ReplacementMap replacement : this.gTNHLib$replacements) {
                NBTTagCompound replacements = new NBTTagCompound();
                if (replacement.input() == null) continue;
                replacements.setTag("input", replacement.input().writeToNBT(new NBTTagCompound()));

                ItemStack output = replacement.output();
                if (output != null) {
                    replacements.setTag("output", output.writeToNBT(new NBTTagCompound()));
                } else {
                    replacements.setBoolean("nullOutput", true);
                }

                replacements.setBoolean("strict", replacement.strict());
                nbttaglist.appendTag(replacements);
            }
            nbtCompound.setTag("replacements", nbttaglist);
        }
    }

    // write to NBT with the other ingredients and stuff
    @WrapMethod(method = "readFromNBT")
    public void readFromNBT(NBTTagCompound nbtCompound, Operation<Void> original) {
        original.call(nbtCompound);
        if (!nbtCompound.hasKey("replacements")) {
            return;
        }
        gTNHLib$replacements.clear();
        NBTTagList nbttaglist = nbtCompound.getTagList("replacements", 10); // the list of replacements
        for (int i = 0; i < nbttaglist.tagCount(); ++i) { // for every replacement entry in replacements
            NBTTagCompound replacementEntry = nbttaglist.getCompoundTagAt(i); // get the replacament entry
            ItemStack input = ItemStack.loadItemStackFromNBT(replacementEntry.getCompoundTag("input"));
            ItemStack output = null;
            if (!replacementEntry.getBoolean("nullOutput"))
                output = ItemStack.loadItemStackFromNBT(replacementEntry.getCompoundTag("output"));

            boolean strict = replacementEntry.getBoolean("strict");

            this.gTNHLib$replacements.add(new EnhancedInfusionRecipe.ReplacementMap(input, output, strict));
        }
    }

}
