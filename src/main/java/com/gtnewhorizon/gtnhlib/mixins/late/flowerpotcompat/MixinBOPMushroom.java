package com.gtnewhorizon.gtnhlib.mixins.late.flowerpotcompat;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.api.IFlowerPottable;

import biomesoplenty.common.blocks.BlockBOPMushroom;

@Mixin(value = BlockBOPMushroom.class, remap = false)
@Implements(@Interface(iface = IFlowerPottable.class, prefix = "gtnhlib$"))
public class MixinBOPMushroom {

    public boolean gtnhlib$isFlowerPottable(int meta) {
        // These ones don't work well, they clip way too badly
        // Glowshroom, Shadow Shroom
        return (meta != 3 && meta != 5);
    }
}
