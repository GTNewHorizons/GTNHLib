package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.api.IFlowerPottable;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(RenderBlocks.class)
public class MixinRenderBlocks_FlowerPot {

    @Shadow
    public IBlockAccess blockAccess;

    @Inject(
            method = "renderBlockFlowerpot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getBlockFromItem(Lnet/minecraft/item/Item;)Lnet/minecraft/block/Block;"),
            cancellable = true)
    private void gtnhlib$renderCustomFlowerPot(BlockFlowerPot blockPot, int x, int y, int z,
            CallbackInfoReturnable<Boolean> cir, @Local(name = "tileentity") TileEntity tileentity) {
        if (!(tileentity instanceof TileEntityFlowerPot pot)) return;

        Item item = pot.getFlowerPotItem();
        if (item == null) return;

        Block block = Block.getBlockFromItem(item);
        if (!(block instanceof IFlowerPottable pottable)) return;

        NBTTagCompound compound = new NBTTagCompound();
        pot.writeToNBT(compound);

        if (pottable.renderFlowerPot(compound, blockAccess, block, x, y, z, (RenderBlocks) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
