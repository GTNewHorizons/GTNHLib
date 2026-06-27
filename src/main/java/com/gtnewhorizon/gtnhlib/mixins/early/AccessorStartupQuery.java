package com.gtnewhorizon.gtnhlib.mixins.early;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import cpw.mods.fml.common.StartupQuery;

@Mixin(StartupQuery.class)
public interface AccessorStartupQuery {

    @Accessor(value = "text", remap = false)
    void gtnhlib$setText(String text);
}
