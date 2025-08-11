package com.gtnewhorizon.gtnhlib.core;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import cpw.mods.fml.relauncher.FMLRelaunchLog;

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
        // We register this transformer here so that it
        // runs after Mixins, but before LWJGl3ify
        String transformer = "com.gtnewhorizon.gtnhlib.core.transformer.TessellatorRedirectorTransformer";
        FMLRelaunchLog.finer("Registering transformer %s", transformer);
        Launch.classLoader.registerTransformer(transformer);
        return new String[0];
    }
}
