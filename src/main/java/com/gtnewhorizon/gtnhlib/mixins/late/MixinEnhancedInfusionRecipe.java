package com.gtnewhorizon.gtnhlib.mixins.late;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.oredict.OreDictionary;

import org.objectweb.asm.Opcodes;
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
    private List<EnhancedInfusionRecipe.Replacement> gTNHLib$replacements = new ArrayList<>();

    // Save the replacements for use in itemReplacement
    @Inject(
            method = "craftingStart",
            at = @At(
                    value = "FIELD",
                    target = "Lthaumcraft/common/tiles/TileInfusionMatrix;recipeType:I",
                    ordinal = 0,
                    opcode = Opcodes.PUTFIELD))
    public void setRecipe(CallbackInfo ci, @Local InfusionRecipe recipe) {
        if (recipe instanceof EnhancedInfusionRecipe r) {
            this.gTNHLib$replacements = r.getReplacements();
        }
    }

    // If one exists, replace the item on the pedestal with its replacement and skip the normal consumption logic
    @Inject(
            method = "craftCycle",
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lthaumcraft/common/tiles/TilePedestal;getStackInSlot(I)Lnet/minecraft/item/ItemStack;",
                    ordinal = 4,
                    remap = true))
    public void itemReplacement(CallbackInfo ci, @Local TileEntity te, @Local int slot) {
        if (this.gTNHLib$replacements.isEmpty()) {
            return;
        }
        TilePedestal pedestal = (TilePedestal) te;
        ItemStack stack = pedestal.getStackInSlot(0);
        EnhancedInfusionRecipe.Replacement replacement = gTNHLib$getReplacement(stack);
        if (replacement == EnhancedInfusionRecipe.NO_REPLACEMENT) return;
        this.recipeIngredients.remove(slot);
        if (!OreDictionary.itemMatches(stack, replacement.output(), replacement.strict())) {
            pedestal.setInventorySlotContents(0, replacement.output());
        }
        ci.cancel();
    }

    @Inject(method = "craftingFinish", at = @At(value = "RETURN"))
    public void clearRecipe(CallbackInfo ci) {
        this.gTNHLib$replacements.clear();
    }

    // Remap the injection because writeToNBT is a MC TileEntity method
    @Inject(method = "writeToNBT", at = @At(value = "RETURN"), remap = true)
    public void writeToNBT(NBTTagCompound nbtCompound, CallbackInfo ci) {
        if (this.gTNHLib$replacements == null || this.gTNHLib$replacements.isEmpty()) {
            return;
        }
        NBTTagList nbttaglist = new NBTTagList();
        for (EnhancedInfusionRecipe.Replacement replacement : this.gTNHLib$replacements) {
            if (replacement.input() == null) continue;
            NBTTagCompound replacements = new NBTTagCompound();

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

    // Remap the injection because writeToNBT is a MC TileEntity method
    @Inject(method = "readFromNBT", at = @At(value = "RETURN"), remap = true)
    public void readFromNBT(NBTTagCompound nbtCompound, CallbackInfo ci) {
        if (!nbtCompound.hasKey("replacements")) {
            return;
        }
        gTNHLib$replacements.clear();
        NBTTagList nbttaglist = nbtCompound.getTagList("replacements", 10); // the list of replacements
        for (int i = 0; i < nbttaglist.tagCount(); ++i) { // for every replacement entry in replacements
            NBTTagCompound replacementEntry = nbttaglist.getCompoundTagAt(i); // get the replacament entry
            ItemStack input = ItemStack.loadItemStackFromNBT(replacementEntry.getCompoundTag("input"));
            ItemStack output = null;
            if (!replacementEntry.getBoolean("nullOutput")) {
                output = ItemStack.loadItemStackFromNBT(replacementEntry.getCompoundTag("output"));
            }

            boolean strict = replacementEntry.getBoolean("strict");

            this.gTNHLib$replacements.add(new EnhancedInfusionRecipe.Replacement(input, output, strict));
        }
    }

    /**
     * @param key The ItemStack to find a Replacement for.
     * @return the Replacement for the given ItemStack. EnhancedInfusionRecipe.NO_REPLACEMENT if no replacement was
     *         found.
     */
    @Unique
    public EnhancedInfusionRecipe.Replacement gTNHLib$getReplacement(ItemStack key) {
        for (EnhancedInfusionRecipe.Replacement replacement : this.gTNHLib$replacements) {
            if (OreDictionary.itemMatches(replacement.input(), key, replacement.strict())) {
                return replacement;
            }
        }
        return EnhancedInfusionRecipe.NO_REPLACEMENT;
    }

}
