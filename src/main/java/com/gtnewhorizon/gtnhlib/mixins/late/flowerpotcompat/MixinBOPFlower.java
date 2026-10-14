package com.gtnewhorizon.gtnhlib.mixins.late.flowerpotcompat;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

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
}
