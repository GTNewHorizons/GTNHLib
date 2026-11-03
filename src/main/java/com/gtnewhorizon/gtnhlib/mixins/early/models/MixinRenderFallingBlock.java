package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderFallingBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.api.BlockModelInfo;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(RenderFallingBlock.class)
public class MixinRenderFallingBlock {

    @WrapOperation(
            method = "doRender(Lnet/minecraft/entity/item/EntityFallingBlock;DDDFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockSandFalling(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;IIII)V"))
    private void nhlib$renderFallingModel(RenderBlocks renderer, Block block, World world, int x, int y, int z,
            int meta, Operation<Void> original) {
        if (((BlockModelInfo) block).nhlib$isModeled()) {
            ModelISBRH.INSTANCE.get().renderFallingBlock(world, block, x, y, z, meta, renderer);
            return;
        }

        original.call(renderer, block, world, x, y, z, meta);
    }
}
