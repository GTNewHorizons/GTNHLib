package com.gtnewhorizon.gtnhlib.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class PreInitMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger("GTNHLib|PreInit");

    @Override
    public void onLoad(String mixinPackage) {
        // Mixingasm keeps untrusted coremod transformers out of Mixin's metadata passes, but ships as a DEFAULT-env
        // Force it to run here. Stops pre-init mixins from causing trouble with Asjcore
        try {
            Class.forName("makamys.mixingasm.Mixingasm").getMethod("run").invoke(null);
        } catch (ClassNotFoundException absent) {
            LOGGER.warn(
                    "Mixingasm not present; PREINIT mixin metadata passes will run untrusted coremod transformers, "
                            + "which can break mods that patch classes only once");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not run Mixingasm early; PREINIT metadata passes stay unprotected", e);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();

        mixins.add("MixinGameData_WorldConversionWarning");

        return mixins;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
