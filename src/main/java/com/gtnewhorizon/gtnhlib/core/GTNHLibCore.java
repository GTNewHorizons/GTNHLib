package com.gtnewhorizon.gtnhlib.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(-1000)
public class GTNHLibCore implements IFMLLoadingPlugin {
    /*
     * Doesn't currently do anything, other than force the mod to load with coremods so Hodgepodge can use it's functions in mixins/asm
     */
    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
