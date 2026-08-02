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

import biomesoplenty.common.blocks.BlockBOPFlower2;

@Mixin(value = BlockBOPFlower2.class, remap = false)
@Implements(@Interface(iface = IFlowerPottable.class, prefix = "gtnhlib$"))
public class MixinBOPFlower2 {

    public boolean gtnhlib$isFlowerPottable(int meta) {
        // These ones don't work well, they clip way too badly
        // Lily of the Valley, Bluebells
        return (meta != 1 && meta != 5);
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
            case 2 -> 0.55F;
            case 3 -> 0.65F;
            case 7 -> 0.65F;
            default -> 0.75F;
        };
    }
}
