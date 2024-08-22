package com.gtnewhorizon.gtnhlib.core;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.gtnewhorizon.gtnhlib.compat.FalseTweaksCompat;
import com.gtnewhorizon.gtnhlib.core.transformer.TessellatorRedirectorTransformer;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

public class MixinCompatHackTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        // no-op
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        // no-op
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        if (FMLLaunchHandler.side().isClient()) {
            final boolean rfbLoaded = Launch.blackboard.getOrDefault("gtnhlib.rfbPluginLoaded", Boolean.FALSE)
                    == Boolean.TRUE;

            if (!rfbLoaded && !FalseTweaksCompat.threadingActive() && !(Loader.isModLoaded("Optifine") || Loader.isModLoaded("FastCraft"))) {
                // Run after Mixins, but before LWJGl3ify
                Launch.classLoader.registerTransformer(TessellatorRedirectorTransformer.class.getName());
            }
        }

        return new String[0];
    }
}
