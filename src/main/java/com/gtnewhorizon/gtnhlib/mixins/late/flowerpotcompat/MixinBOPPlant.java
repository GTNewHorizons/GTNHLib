package com.gtnewhorizon.gtnhlib.mixins.late.flowerpotcompat;

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
}
