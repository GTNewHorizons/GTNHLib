package com.gtnewhorizon.gtnhlib.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;

import com.gtnewhorizon.gtnhlib.mixins.Mixins;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "com.gtnewhorizon.gtnhlib.asm.", "com.gtnewhorizon.gtnhlib.core.transformer",
        "com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager",
        "com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator" })
public class GTNHLibCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        if (FMLLaunchHandler.side().isClient()) {
            boolean isGTNHLibRFBLoaded = (boolean) Launch.blackboard
                    .getOrDefault("gtnhlib.rfbPluginLoaded", Boolean.FALSE);
            if (!isGTNHLibRFBLoaded) {
                // If rfb isn't loaded we need to register the TessellatorRedirectorTransformer
                // transformer, however this transformer needs to run late in the transformer
                // chain, after mixins but before LWJGl3ify. If we were to register it normally
                // in this method it would be sorted at index 0 which we do not want. So we
                // instead register it inside an ITweaker that gets run by mixins.
                List<String> tweaks = GlobalProperties.get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKCLASSES);
                if (tweaks != null) {
                    tweaks.add("com.gtnewhorizon.gtnhlib.core.tweaks.LateTransformerRegistrationTweaker");
                }
            }
        }
        return new String[] { "com.gtnewhorizon.gtnhlib.core.transformer.EventBusSubTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return "com.gtnewhorizon.gtnhlib.core.GTNHLibCoreModContainer";
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
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }

    @Override
    public String toString() {
        // Needed because the EarlyMixins loader uses the
        // Object.toString() method to indentify coremods...
        return "GTNHLib Core";
    }
}
