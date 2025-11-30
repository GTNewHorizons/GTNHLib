package com.gtnewhorizon.gtnhlib.mixins.early.models;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Block.class)
public class MixinBlockParticle {

    @WrapMethod(method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;")
    private IIcon nhlib$wrapGetIcon(IBlockAccess world, int x, int y, int z, int side, Operation<IIcon> original) {
        Block block = (Block) (Object) this;
        int meta = world.getBlockMetadata(x, y, z);
        if (block.getRenderType() == ModelISBRH.JSON_ISBRH_ID) {
            return ModelISBRH.INSTANCE.getParticleIcon(block, world, x, y, z, meta);
        }
        return original.call(world, x, y, z, side);
    }

    @WrapMethod(method = "getIcon(II)Lnet/minecraft/util/IIcon;")
    private IIcon nhlib$wrapGetIcon(int side, int meta, Operation<IIcon> original) {
        Block block = (Block) (Object) this;
        if (block.getRenderType() == ModelISBRH.JSON_ISBRH_ID) {
            return ModelISBRH.INSTANCE.getParticleIcon(block, null, 0, 0, 0, meta);
        }
        return original.call(meta, side);
    }
}
