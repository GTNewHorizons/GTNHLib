package com.gtnewhorizon.gtnhlib.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.GTNHLib;

/**
 * This interface needs to be implemented on an enum that declares all your mixins
 */
public interface IMixins {

    MixinBuilder getBuilder();

    // spotless:off
    /**
     * Returns the list of mixins that should be loaded early by GTNH mixins. This method needs to be called in a class that implements
     * both {@link cpw.mods.fml.relauncher.IFMLLoadingPlugin} and {@link com.gtnewhorizon.gtnhlib.mixin.IMixins}. You
     * may call it as such :
     *
     * <pre>
     * {@code
     *     @Override
     *     public List<String> getMixins(Set<String> loadedCoreMods) {
     *         return IMixins.getEarlyMixins(YourMixinEnum.class, loadedCoreMods);
     *     }
     * }
     * </pre>
     */
    static <E extends Enum<E> & IMixins> List<String> getEarlyMixins(Class<E> enumClass, Set<String> loadedCoreMods) {
        final List<String> mixinsToLoad = new ArrayList<>();
        final List<String> mixinsToNotLoad = new ArrayList<>();
        for (E mixin : enumClass.getEnumConstants()) {
            mixin.getBuilder().validateBuilder(mixin);
            mixin.getBuilder().loadEarlyMixins(loadedCoreMods, mixinsToLoad, mixinsToNotLoad);
        }
        GTNHLib.LOG.info("Not loading the following EARLY mixins: {}", mixinsToNotLoad);
        return mixinsToLoad;
    }

    /**
     * Returns the list of mixins that should be loaded late by GTNH mixins. This method needs to be called in a class annotated with
     * {@link com.gtnewhorizon.gtnhmixins.LateMixin} and implementing
     * {@link com.gtnewhorizon.gtnhmixins.ILateMixinLoader}. You may call it as such :
     *
     * <pre>
     * {@code
     *     @Override
     *     public List<String> getMixins(Set<String> loadedMods) {
     *         return IMixins.getLateMixins(YourMixinEnum.class, loadedMods);
     *     }
     * }
     * </pre>
     */
    static <E extends Enum<E> & IMixins> List<String> getLateMixins(Class<E> enumClass, Set<String> loadedMods) {
        final List<String> mixinsToLoad = new ArrayList<>();
        final List<String> mixinsToNotLoad = new ArrayList<>();
        for (E mixin : enumClass.getEnumConstants()) {
            mixin.getBuilder().validateBuilder(mixin);
            mixin.getBuilder().loadLateMixins(loadedMods, mixinsToLoad, mixinsToNotLoad);
        }
        GTNHLib.LOG.info("Not loading the following LATE mixins: {}", mixinsToNotLoad.toString());
        return mixinsToLoad;
    }
    // spotless:on

    /**
     * Phase is used for GTNH mixins
     */
    enum Phase {
        EARLY,
        LATE
    }

}
