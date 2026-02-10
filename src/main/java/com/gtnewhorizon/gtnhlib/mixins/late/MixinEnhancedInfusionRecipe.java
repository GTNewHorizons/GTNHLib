package com.gtnewhorizon.gtnhlib.mixins.late;

import com.gtnewhorizon.gtnhlib.api.thaumcraft.EnhancedInfusionRecipe;
import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.entities.EntityPermanentItem;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TilePedestal;

@Mixin(value = TileInfusionMatrix.class, remap = false)
public abstract class MixinEnhancedInfusionRecipe {
    @Unique
    private InfusionRecipe recipe;

    // Get the recipe from the crafting method
    @Inject (method = "craftingStart",
        at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;recipeType:I"))
    public void getRecipe(EntityPlayer player, CallbackInfo ci) {
        this.recipe = recipe;
    }

    //if recipe is instanceOf EnhancedInfusionRecipe, consume item and pop off the corresponding replacement item, if defined
    /*@Inject (method = "craftingCycle",
        at = @At(value = "FIELD", target = "Lthaumcraft/common/tiles/TileInfusionMatrix;itemCount:I", ordinal = 2, shift = "BY", by = 2))
    public void popoffReplacement(EntityPlayer player, CallbackInfo ci) {
        if (this.recipe instanceof EnhancedInfusionRecipe)
            if (recipe.replacements.containsKey(((TilePedestal)te).getStackInSlot(0).getItem().getContainerItem(((TilePedestal)te).getStackInSlot(0)))) {
                EntitySpecialItem entityitem = new EntityPermanentItem(
                    this.worldObj,
                    cc.posX, cc.posY, cc.posZ,
                    new ItemStack(
                        recipe.replacements[((TilePedestal)te).getStackInSlot(0).getItem().getContainerItem(((TilePedestal)te).getStackInSlot(0))]));
                entityitem.motionX = entityitem.motionY = entityitem.motionZ = 0;
                this.worldObj.spawnEntityInWorld(entityitem);
            }

    }*/


}
