package com.gtnewhorizon.gtnhlib.mixins;

import javax.annotation.Nonnull;

import cpw.mods.fml.common.Loader;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetMods implements ITargetMod {

    LWJGL3IFY("me.eigenraven.lwjgl3ify.core.Lwjgl3ifyCoremod"),
    THAUMCRAFT( "", "Thaumcraft");

    private final TargetModBuilder builder;

    TargetMods(String coreModClass) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass);
    }

    TargetMods(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass).setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
