package com.gtnewhorizon.gtnhlib.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

public interface IMixins {

    List<String> getMixinClasses();

    Supplier<Boolean> getApplyIf();

    Phase getPhase();

    Side getSide();

    List<ITargetedMod> getTargetedMods();

    List<ITargetedMod> getExcludedMods();

    static <E extends Enum<E> & IMixins> List<E> getAllValues(Class<E> enumClass) {
        if (enumClass.isEnum() && IMixins.class.isAssignableFrom(enumClass)) {
            return Arrays.asList(enumClass.getEnumConstants());
        }
        return Collections.emptyList();
    }

    static <E extends Enum<E> & IMixins> List<String> getEarlyMixins(Class<E> enumClass, Set<String> loadedCoreMods) {
        final List<String> mixins = new ArrayList<>();
        final List<String> notLoading = new ArrayList<>();
        for (E mixin : getAllValues(enumClass)) {
            if (mixin.getPhase() == Phase.EARLY) {
                if (mixin.shouldLoad(loadedCoreMods, Collections.emptySet())) {
                    mixins.addAll(mixin.getMixinClasses());
                } else {
                    notLoading.addAll(mixin.getMixinClasses());
                }
            }
        }
        GTNHLib.LOG.info("Not loading the following EARLY mixins: {}", notLoading);
        return mixins;
    }

    static <E extends Enum<E> & IMixins> List<String> getLateMixins(Class<E> enumClass, Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        final List<String> notLoading = new ArrayList<>();
        for (E mixin : getAllValues(enumClass)) {
            if (mixin.getPhase() == Phase.LATE) {
                if (mixin.shouldLoad(Collections.emptySet(), loadedMods)) {
                    mixins.addAll(mixin.getMixinClasses());
                } else {
                    notLoading.addAll(mixin.getMixinClasses());
                }
            }
        }
        GTNHLib.LOG.info("Not loading the following LATE mixins: {}", notLoading.toString());
        return mixins;
    }

    default boolean shouldLoadSide() {
        return getSide() == Side.BOTH || (getSide() == Side.SERVER && FMLLaunchHandler.side().isServer())
                || (getSide() == Side.CLIENT && FMLLaunchHandler.side().isClient());
    }

    default boolean allModsLoaded(List<ITargetedMod> targetedMods, Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (targetedMods.isEmpty()) return false;

        for (ITargetedMod target : targetedMods) {
            if (target == TargetedMod.VANILLA) continue;

            // Check coremod first
            if (!loadedCoreMods.isEmpty() && target.getCoreModClass() != null
                    && !loadedCoreMods.contains(target.getCoreModClass()))
                return false;
            else if (!loadedMods.isEmpty() && target.getModId() != null && !loadedMods.contains(target.getModId()))
                return false;
        }

        return true;
    }

    default boolean noModsLoaded(List<ITargetedMod> targetedMods, Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (targetedMods.isEmpty()) return true;

        for (ITargetedMod target : targetedMods) {
            if (target == TargetedMod.VANILLA) continue;

            // Check coremod first
            if (!loadedCoreMods.isEmpty() && target.getCoreModClass() != null
                    && loadedCoreMods.contains(target.getCoreModClass()))
                return false;
            else if (!loadedMods.isEmpty() && target.getModId() != null && loadedMods.contains(target.getModId()))
                return false;
        }

        return true;
    }

    default boolean shouldLoad(Set<String> loadedCoreMods, Set<String> loadedMods) {
        return (shouldLoadSide() && getApplyIf().get()
                && allModsLoaded(getTargetedMods(), loadedCoreMods, loadedMods)
                && noModsLoaded(getExcludedMods(), loadedCoreMods, loadedMods));
    }

}
