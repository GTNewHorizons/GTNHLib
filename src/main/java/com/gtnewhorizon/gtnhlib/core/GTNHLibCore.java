package com.gtnewhorizon.gtnhlib.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.falsepattern.deploader.DeploaderStub;
import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.mixins.Mixins;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "it.unimi.dsi.fastutil", "com.gtnewhorizon.gtnhlib.asm",
        "com.gtnewhorizon.gtnhlib.core", "com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager",
        "com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator" })
public class GTNHLibCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    private static final String JVMDG_SYSCL_MARKER = "gtnhlib.jvmdg.systemClassLoader";
    private static final String JVMDG_ARTIFACT = "jvmdowngrader-java-api";
    private static final String RFB_PACKAGE = "com.gtnewhorizons.retrofuturabootstrap";

    static {
        try {
            // Delegate to GTNHExtLib if present. Should be safe to class load since it would have been loaded by FML
            // earlier based on alphabetical naming/coremods being on the class path
            Class.forName("com.gtnewhorizons.gtnhextlib.core.GTNHExtLibCore", true, Launch.classLoader);
        } catch (ClassNotFoundException notExtLib) {
            // Otherwise do that work ourselves
            removeBrigadierClassLoaderException();
            loadDependencies();
        }
    }

    /// Lifted here so we can safely use it in Mixins. This class should be loaded by the time Mixins fire.
    public static final Logger MODEL_LOGGER = LogManager.getLogger("GTNHLib|Models");
    private static Boolean isObf;

    public GTNHLibCore() {
        verifyDependencies();
        try {
            ConfigurationManager.registerConfig(GTNHLibConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    private static void verifyDependencies() {
        // Check for MixinExtras Expression support (added in UniMixins 0.1.23)
        if (GTNHLibCore.class.getResource("/com/llamalad7/mixinextras/expression/Expression.class") == null) {
            throw new RuntimeException(
                    "UniMixins is outdated: GTNHLib requires UniMixins 0.1.23 or newer! Download the unimixins-all jar (not -dev) from: https://github.com/LegacyModdingMC/UniMixins/releases");
        }
    }

    private static void removeBrigadierClassLoaderException() {
        try {
            Field cleF = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            cleF.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> cle = (Set<String>) cleF.get(Launch.classLoader);
            // for Brigadier
            cle.remove("com.mojang.");
            // Thermos console log compat
            boolean hybridServer = Launch.classLoader.getResource("org/bukkit/World.class") != null
                    || Launch.classLoader.getResource("thermos/Thermos.class") != null;
            if (hybridServer) {
                cle.add("com.mojang.util.QueueLogAppender");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadDependencies() {
        DeploaderStub.bootstrap(false);
        DeploaderStub.runDepLoader();
        mirrorJvmdgStubToSystemClassLoader();
    }

    private static void mirrorJvmdgStubToSystemClassLoader() {
        final Logger log = LogManager.getLogger("GTNHLib");
        if (Launch.blackboard.get(JVMDG_SYSCL_MARKER) != null) {
            return;
        }
        final ClassLoader scl = ClassLoader.getSystemClassLoader();
        if (!(scl instanceof URLClassLoader) || scl.getClass().getName().startsWith(RFB_PACKAGE)) {
            return;
        }
        try {
            final Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            int mirrored = 0;
            for (URL url : Launch.classLoader.getSources()) {
                final String path = url.getPath();
                if (path == null || !path.contains(JVMDG_ARTIFACT)) {
                    continue;
                }
                addURL.invoke(scl, url);
                mirrored++;
                log.info("Mirrored jvmdg stub {} onto system classloader", url);
            }
            if (mirrored == 0) {
                log.warn("jvmdg stub not found on LaunchClassLoader sources; system-classloader mirror skipped");
            } else {
                Launch.blackboard.put(JVMDG_SYSCL_MARKER, Boolean.TRUE);
            }
        } catch (ReflectiveOperationException e) {
            log.error("Failed to mirror jvmdg stub onto system classloader", e);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "com.gtnewhorizon.gtnhlib.core.fml.transformers.EventBusSubTransformer",
                "com.gtnewhorizon.gtnhlib.core.fml.transformers.BlockIconTransformer" };
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
    public void injectData(Map<String, Object> data) {
        isObf = (Boolean) data.get("runtimeDeobfuscationEnabled");
        if (FMLLaunchHandler.side().isClient()) {
            boolean isGTNHLibRFBLoaded = (boolean) Launch.blackboard
                    .getOrDefault("gtnhlib.rfbPluginLoaded", Boolean.FALSE);
            if (!isGTNHLibRFBLoaded) {
                // If rfb isn't loaded we need to register the FMLTessellatorRedirectorWrapper
                // transformer, however this transformer needs to run late in the transformer
                // chain, after mixins but before LWJGl3ify. If we were to register it normally
                // in getASMTransformerClass() it would be sorted at index 0 which we do not want.
                // So we instead register it inside an ITweaker that gets run by mixins.
                final List<String> tweaks = (List<String>) Launch.blackboard.get("TweakClasses");
                if (tweaks != null) {
                    tweaks.add("com.gtnewhorizon.gtnhlib.core.fml.tweakers.LateTransformerRegistrationTweaker");
                }
            }
        }
    }

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

    public static boolean isObf() {
        if (isObf == null) {
            throw new IllegalStateException("Obfuscation state has been accessed too early!");
        }
        return isObf;
    }
}
