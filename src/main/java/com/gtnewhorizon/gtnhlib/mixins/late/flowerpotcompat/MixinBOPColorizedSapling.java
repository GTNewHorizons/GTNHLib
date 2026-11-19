package com.gtnewhorizon.gtnhlib.mixins.late.flowerpotcompat;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.api.IFlowerPottable;

import biomesoplenty.common.blocks.BlockBOPColorizedSapling;

@Mixin(value = BlockBOPColorizedSapling.class, remap = false)
@Implements(@Interface(iface = IFlowerPottable.class, prefix = "gtnhlib$"))
public class MixinBOPColorizedSapling {

}
