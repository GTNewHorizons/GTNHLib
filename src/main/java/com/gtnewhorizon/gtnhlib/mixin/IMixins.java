package com.gtnewhorizon.gtnhlib.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.gtnewhorizon.gtnhlib.GTNHLib;

/**
 * This interface needs to be implemented on an enum that declares all your mixins
 */
@SuppressWarnings("unused")
public interface IMixins {

    @Nonnull
    default MixinBuilder getBuilder() {
        return new MixinBuilder(this);
    }

    // spotless:off
    default @Nullable List<String> getCommonMixins() {return null;}
    default @Nullable List<String> getClientMixins() {return null;}
    default @Nullable List<String> getServerMixins() {return null;}
    default @Nullable List<ITargetedMod> getRequiredMods() {return null;}
    default @Nullable List<ITargetedMod> getExcludedMods() {return null;}
    default @Nullable Phase getPhase() {return null;}
    default Supplier<Boolean> getApplyIf() {return null;}

    /**
     * Returns the list of mixins that should be loaded from your {@link org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin} implementation.
     * <p>
     * You may call it as such :
     * <pre>
     * {@code
     *     @Override
     *     public List<String> getMixins() {
     *         return IMixins.getMixins(YourMixinEnum.class);
     *     }
     * }
     * </pre>
     */
    static <E extends Enum<E> & IMixins> List<String> getMixins(Class<E> enumClass) {
        final List<String> mixinsToLoad = new ArrayList<>();
        final List<String> mixinsToNotLoad = new ArrayList<>();
        for (E mixin : enumClass.getEnumConstants()) {
            mixin.getBuilder().loadMixins(mixin, mixinsToLoad, mixinsToNotLoad);
        }
        GTNHLib.LOG.info("Not loading the following mixins: {}", mixinsToNotLoad);
        return mixinsToLoad;
    }

    /**
     * Returns the list of mixins that should be loaded from your {@link com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader} implementation.
     * Note that you also need to implement {@link cpw.mods.fml.relauncher.IFMLLoadingPlugin} for early mixins to work.
     * <p>
     * You may call it as such :
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
            mixin.getBuilder().loadEarlyMixins(mixin, loadedCoreMods, mixinsToLoad, mixinsToNotLoad);
        }
        GTNHLib.LOG.info("Not loading the following EARLY mixins: {}", mixinsToNotLoad);
        return mixinsToLoad;
    }

    /**
     * Returns the list of mixins that should be loaded from your {@link com.gtnewhorizon.gtnhmixins.ILateMixinLoader} implementation.
     * Note that you also need to annotate your class with {@link com.gtnewhorizon.gtnhmixins.LateMixin} for late mixins to work.
     * <p>
     * You may call it as such :
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
            mixin.getBuilder().loadLateMixins(mixin, loadedMods, mixinsToLoad, mixinsToNotLoad);
        }
        GTNHLib.LOG.info("Not loading the following LATE mixins: {}", mixinsToNotLoad.toString());
        return mixinsToLoad;
    }
    // spotless:on

    /**
     * Phase is only used by early and late mixins from gtnh mixins
     */
    enum Phase {
        EARLY,
        LATE
    }

}
