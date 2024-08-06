package com.gtnewhorizon.gtnhlib.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhlib.core.transformer.TessellatorRedirectorTransformer;
import com.gtnewhorizon.gtnhlib.mixins.Mixins;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "com.gtnewhorizon.gtnhlib.core.transformer",
        "com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager",
        "com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator" })
@IFMLLoadingPlugin.SortingIndex(-1000)
public class GTNHLibCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        if (FMLLaunchHandler.side().isClient()) {
            final boolean rfbLoaded = Launch.blackboard.getOrDefault("gtnhlib.rfbPluginLoaded", Boolean.FALSE)
                    == Boolean.TRUE;
            if (!rfbLoaded) {
                System.out.println("GTNHLib: RFB plugin not loaded, loading ASM transformer");
                return new String[] { TessellatorRedirectorTransformer.class.getName() };
            } else {
                System.out.println("GTNHLib: RFB plugin loaded, skipping ASM transformer");
            }
        }
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

    @Override
    public String getMixinConfig() {
        return "mixins.gtnhlib.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return Mixins.getEarlyMixins(loadedCoreMods);
    }
}
