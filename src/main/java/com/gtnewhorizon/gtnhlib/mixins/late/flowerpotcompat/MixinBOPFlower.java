package com.gtnewhorizon.gtnhlib.mixins.late.flowerpotcompat;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizon.gtnhlib.api.IFlowerPottable;

import biomesoplenty.common.blocks.BlockBOPFlower;

@Mixin(value = BlockBOPFlower.class, remap = false)
@Implements(@Interface(iface = IFlowerPottable.class, prefix = "gtnhlib$"))
public class MixinBOPFlower {

    public boolean gtnhlib$isFlowerPottable(int meta) {
        // These ones don't work well, they clip way too badly or are flat on the ground
        // Clover, Swampflower, Violet, White Anemone
        return (meta != 0 && meta != 1 && meta != 8 && meta != 9);
    }

    public boolean gtnhlib$renderFlowerPot(NBTTagCompound compound, IBlockAccess blockAccess, Block block, int x, int y,
            int z, RenderBlocks render) {
        Tessellator tess = Tessellator.instance;
        tess.addTranslation(0, 4F / 16F, 0);
        render.drawCrossedSquares(
                block.getIcon(blockAccess, x, y, z, 0),
                x,
                y,
                z,
                getScale(blockAccess.getBlockMetadata(x, y, z)));
        tess.addTranslation(0, -4F / 16F, 0);
        return true;
    }

    @Unique
    private float getScale(int meta) {
        return switch (meta) {
            case 4 -> 0.7F;
            case 11, 12 -> 0.45F;
            default -> 0.75F;
        };
    }
}
