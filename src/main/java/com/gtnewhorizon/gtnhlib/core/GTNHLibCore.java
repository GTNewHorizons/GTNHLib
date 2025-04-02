package com.gtnewhorizon.gtnhlib.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.service.mojang.MixinServiceLaunchWrapper;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.gtnewhorizon.gtnhlib.Tags;
import com.gtnewhorizon.gtnhlib.client.tooltip.LoreHolderDiscoverer;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusUtil;
import com.gtnewhorizon.gtnhlib.mixin.IMixins;
import com.gtnewhorizon.gtnhlib.mixins.Mixins;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "com.gtnewhorizon.gtnhlib.asm.", "com.gtnewhorizon.gtnhlib.core.transformer",
        "com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager",
        "com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator" })
@IFMLLoadingPlugin.SortingIndex(-1000)
public class GTNHLibCore extends DummyModContainer implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public static final String[] DEFAULT_TRANSFORMERS = new String[] {
            "com.gtnewhorizon.gtnhlib.core.transformer.EventBusSubTransformer" };

    public GTNHLibCore() {
        super(new ModMetadata());
        ModMetadata md = getMetadata();
        md.autogenerated = true;
        md.modId = md.name = "GTNHLib Core";
        md.parent = "gtnhlib";
        md.version = Tags.VERSION;
    }

    @Override
    public String[] getASMTransformerClass() {
        if (!FMLLaunchHandler.side().isClient()
                || Launch.blackboard.getOrDefault("gtnhlib.rfbPluginLoaded", Boolean.FALSE) == Boolean.TRUE) {
            // Don't need any transformers if we're not on the client, or the RFB Plugin was loaded
            return DEFAULT_TRANSFORMERS;
        }
        // Directly add this to the MixinServiceLaunchWrapper tweaker's list of Tweak Classes
        List<String> mixinTweakClasses = GlobalProperties.get(MixinServiceLaunchWrapper.BLACKBOARD_KEY_TWEAKCLASSES);
        if (mixinTweakClasses != null) {
            mixinTweakClasses.add(MixinCompatHackTweaker.class.getName());
        }
        return DEFAULT_TRANSFORMERS;
    }

    @Override
    public String getModContainerClass() {
        return "com.gtnewhorizon.gtnhlib.core.GTNHLibCore";
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
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void construct(FMLConstructionEvent event) {
        EventBusUtil.harvestData(event.getASMHarvestedData());
        if (event.getSide().isClient()) {
            LoreHolderDiscoverer.harvestData(event.getASMHarvestedData());
        }
    }
}
