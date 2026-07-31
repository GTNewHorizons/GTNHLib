package com.gtnewhorizon.gtnhlib.mixins.early;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.api.IFlowerPottable;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(BlockFlowerPot.class)
public class MixinBlockFlowerPot {

    @Inject(method = "func_149928_a(Lnet/minecraft/block/Block;I)Z", at = @At(value = "HEAD"), cancellable = true)
    private void gtnhlib$allowModdedFlowers(Block p_149928_1_, int p_149928_2_, CallbackInfoReturnable<Boolean> cir) {
        if (p_149928_1_ instanceof IFlowerPottable pottable && pottable.canBePotted(p_149928_2_)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getDrops", at = @At("RETURN"), remap = false, cancellable = true)
    private void gtnhlib$customFlowerPotDrops(World world, int x, int y, int z, int metadata, int fortune,
            CallbackInfoReturnable<ArrayList<ItemStack>> cir, @Local(name = "te") TileEntityFlowerPot te) {
        if (te != null) {
            Item item = te.getFlowerPotItem();
            if (Block.getBlockFromItem(item) instanceof IFlowerPottable pottable) {
                NBTTagCompound compound = new NBTTagCompound();
                te.writeToNBT(compound);

                ArrayList<ItemStack> customDrop = pottable.breakFlowerPot(compound);

                if (customDrop != null) {
                    cir.setReturnValue(customDrop);
                }
            }
        }
    }
}
