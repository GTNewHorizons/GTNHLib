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
    private void gtnhlib$renderCustomFlowerPot(BlockFlowerPot p_147752_1_, int p_147752_2_, int p_147752_3_,
            int p_147752_4_, CallbackInfoReturnable<Boolean> cir, @Local(name = "tileentity") TileEntity tileentity) {

        if (tileentity instanceof TileEntityFlowerPot te) {
            Item item = te.getFlowerPotItem();
            if (item != null) {
                Block block = Block.getBlockFromItem(item);
                if (block instanceof IFlowerPottable pottable) {
                    NBTTagCompound compound = new NBTTagCompound();
                    te.writeToNBT(compound);

                    if (pottable.renderFlowerPot(
                            compound,
                            blockAccess,
                            block,
                            p_147752_2_,
                            p_147752_3_,
                            p_147752_4_,
                            (RenderBlocks) (Object) this)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
