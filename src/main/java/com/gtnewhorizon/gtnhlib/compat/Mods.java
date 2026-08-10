package com.gtnewhorizon.gtnhlib.compat;

import cpw.mods.fml.common.Loader;

public class Mods {

    public static final boolean FALSETWEAKS = Loader.isModLoaded("falsetweaks");
    public static final boolean NEI = Loader.isModLoaded("NotEnoughItems");
    public static final boolean ANGELICA = Loader.isModLoaded("angelica");

    // Covers both Baubles and Baubles-Expanded.
    public static final boolean BAUBLES = Loader.isModLoaded("Baubles");
}
