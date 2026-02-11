package com.gtnewhorizon.gtnhlib.mixins.late;

import com.gtnewhorizon.gtnhlib.api.thaumcraft.EnhancedInfusionRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TilePedestal;

import java.util.ArrayList;
import java.util.Iterator;

@Mixin(value = TileInfusionMatrix.class, remap = false)
public abstract class MixinEnhancedInfusionRecipe extends TileThaumcraft {
    @Shadow
    private ArrayList<ItemStack> recipeIngredients;
    @Unique
    private InfusionRecipe recipe;

    //setter since its complaining
    @Unique
    public void setRecipe (InfusionRecipe recipe) {
        this.recipe = recipe;
    }

    // Get the recipe from the crafting method
    @Inject (method = "craftingStart",
        at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;recipeType:I"))
    public void setRecipe(CallbackInfo ci) {
        setRecipe(recipe);
        // System.out.println("Recipe got pulled! LETS GOOOOOO"); // logger
    }

    //if recipe is instanceOf EnhancedInfusionRecipe, replace item with the corresponding replacement item, if defined
    @Inject (method = "craftCycle", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true,
        at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;itemCount:I", ordinal = 3, shift = At.Shift.BY, by = 2))
    public void itemReplacement(CallbackInfo ci, boolean valid, TileEntity te, int a, Iterator i$, ChunkCoordinates cc) {
        if (this.recipe instanceof EnhancedInfusionRecipe && ((EnhancedInfusionRecipe)recipe).hasReplacement(((TilePedestal) te)
                .getStackInSlot(0).getItem().getContainerItem(((TilePedestal) te).getStackInSlot(0)))) {
            ItemStack is2 = ((TilePedestal) te).getStackInSlot(0).getItem().getContainerItem(((TilePedestal) te).getStackInSlot(0));
            ((TilePedestal) te).setInventorySlotContentsFromInfusion(0, ((EnhancedInfusionRecipe)recipe).getReplacement(is2));
            this.recipeIngredients.remove(a);
            // System.out.println("Replacement was made! LETS GOOOOOO"); // logger
            ci.cancel();
        }
    }
}
