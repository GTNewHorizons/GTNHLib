package com.gtnewhorizon.gtnhlib.compat;

import cpw.mods.fml.common.Loader;

public class Mods {

    public static final boolean FALSETWEAKS = Loader.isModLoaded("falsetweaks");
    public static final boolean NEI = Loader.isModLoaded("NotEnoughItems");
    public static final boolean ANGELICA = Loader.isModLoaded("angelica");

    // GTNH Baubles-Expanded is a drop-in fork that registers under the "Baubles" modid, so one check covers both.
    public static final boolean BAUBLES = Loader.isModLoaded("Baubles");
}
