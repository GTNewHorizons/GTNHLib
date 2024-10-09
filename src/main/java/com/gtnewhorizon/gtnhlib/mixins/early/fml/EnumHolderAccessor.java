package com.gtnewhorizon.gtnhlib.mixins.early.fml;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import cpw.mods.fml.common.discovery.asm.ModAnnotation;

@Mixin(value = ModAnnotation.EnumHolder.class, remap = false)
public interface EnumHolderAccessor {

    @Accessor
    String getValue();
}
