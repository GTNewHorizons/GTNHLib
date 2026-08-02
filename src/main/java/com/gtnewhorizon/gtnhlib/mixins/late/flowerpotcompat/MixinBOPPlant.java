package com.gtnewhorizon.gtnhlib.mixins.late.flowerpotcompat;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.api.IFlowerPottable;

import biomesoplenty.common.blocks.BlockBOPPlant;

@Mixin(value = BlockBOPPlant.class, remap = false)
@Implements(@Interface(iface = IFlowerPottable.class, prefix = "gtnhlib$"))
public class MixinBOPPlant {

    public boolean gtnhlib$isFlowerPottable(int meta) {
        // I just wanted tiny cactus, thorns work well too
        return (meta == 5 || meta == 12);
    }

    public boolean gtnhlib$renderFlowerPot(NBTTagCompound compound, IBlockAccess blockAccess, Block block, int x, int y,
            int z, RenderBlocks render) {
        Tessellator tess = Tessellator.instance;
        tess.addTranslation(0, 4F / 16F, 0);
        render.drawCrossedSquares(block.getIcon(blockAccess, x, y, z, 0), x, y, z, 0.75F);
        tess.addTranslation(0, -4F / 16F, 0);
        return true;
    }
}
