package com.gtnewhorizon.gtnhlib.mixins.late.flowerpotcompat;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

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
}
