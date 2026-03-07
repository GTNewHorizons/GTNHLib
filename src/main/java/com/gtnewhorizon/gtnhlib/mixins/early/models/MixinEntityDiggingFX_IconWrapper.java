package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import cpw.mods.fml.client.registry.RenderingRegistry;

@Mixin(EntityDiggingFX.class)
public class MixinEntityDiggingFX_IconWrapper {

    @WrapOperation(
            method = "<init>(Lnet/minecraft/world/World;DDDDDDLnet/minecraft/block/Block;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getIcon(II)Lnet/minecraft/util/IIcon;"))
    private IIcon nhlib$useWorldIcon(Block block, int side, int meta, Operation<IIcon> original, World world, double x,
            double y, double z, double mx, double my, double mz, Block passedBlock, int passedMeta, int passedSide) {
        if (RenderingRegistry.instance().blockRenderers.get(block.getRenderType()) instanceof ModelISBRH) {
            int bx = (int) Math.floor(x);
            int by = (int) Math.floor(y);
            int bz = (int) Math.floor(z);

            return block.getIcon(world, bx, by, bz, side);
        }

        return original.call(block, side, meta);
    }
}
