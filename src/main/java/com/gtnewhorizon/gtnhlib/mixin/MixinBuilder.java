package com.gtnewhorizon.gtnhlib.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@SuppressWarnings("unused")
public class MixinBuilder {

    private @Nullable List<String> commonMixins;
    private @Nullable List<String> clientMixins;
    private @Nullable List<String> serverMixins;
    private @Nullable List<ITargetedMod> requiredMods;
    private @Nullable List<ITargetedMod> excludedMods;
    private Phase phase;
    private Supplier<Boolean> applyIf = () -> true;

    public MixinBuilder() {}

    public MixinBuilder(String description) {}

    /**
     * Add mixins that should be loaded both on the client and server
     */
    public MixinBuilder addCommonMixins(@Nonnull String... mixins) {
        if (commonMixins == null) commonMixins = new ArrayList<>(4);
        Collections.addAll(commonMixins, mixins);
        return this;
    }

    /**
     * Add mixins that should only be loaded on the client
     */
    public MixinBuilder addClientMixins(@Nonnull String... mixins) {
        if (clientMixins == null) clientMixins = new ArrayList<>(4);
        Collections.addAll(clientMixins, mixins);
        return this;
    }

    /**
     * Add mixins that should only be loaded on the dedicated server
     */
    public MixinBuilder addServerMixins(@Nonnull String... mixins) {
        if (serverMixins == null) serverMixins = new ArrayList<>(4);
        Collections.addAll(serverMixins, mixins);
        return this;
    }

    public MixinBuilder setPhase(Phase phase) {
        this.phase = phase;
        return this;
    }

    public MixinBuilder setApplyIf(Supplier<Boolean> applyIf) {
        this.applyIf = applyIf;
        return this;
    }

    /**
     * Specify mods that need to be present for this mixin to load
     */
    public MixinBuilder addRequiredMod(ITargetedMod mod) {
        if (requiredMods == null) requiredMods = new ArrayList<>(2);
        requiredMods.add(mod);
        return this;
    }

    /**
     * Specify mods that disable this mixin if they are present
     */
    public MixinBuilder addExcludedMod(ITargetedMod mod) {
        if (excludedMods == null) excludedMods = new ArrayList<>(2);
        excludedMods.add(mod);
        return this;
    }

    protected void validateBuilder(Enum<?> entry) {
        int count = 0;
        if (commonMixins != null) count += commonMixins.size();
        if (clientMixins != null) count += clientMixins.size();
        if (serverMixins != null) count += serverMixins.size();
        if (count == 0) {
            throw new RuntimeException("No mixin class registered for IMixins : " + entry.name());
        }
        if (phase == null) {
            throw new RuntimeException("No Phase specified for IMixins : " + entry.name());
        }
    }

    protected void loadEarlyMixins(Set<String> loadedCoreMods, List<String> mixinsToLoad,
            List<String> mixinsToNotLoad) {
        if (phase != Phase.EARLY) return;
        if (shouldLoadMixin(loadedCoreMods, Collections.emptySet())) {
            addMixinsForCurrentSide(mixinsToLoad, mixinsToNotLoad);
        } else {
            addAllMixinsTo(mixinsToNotLoad);
        }
    }

    protected void loadLateMixins(Set<String> loadedMods, List<String> mixinsToLoad, List<String> mixinsToNotLoad) {
        if (phase != Phase.LATE) return;
        if (shouldLoadMixin(Collections.emptySet(), loadedMods)) {
            addMixinsForCurrentSide(mixinsToLoad, mixinsToNotLoad);
        } else {
            addAllMixinsTo(mixinsToNotLoad);
        }
    }

    private boolean shouldLoadMixin(Set<String> loadedCoreMods, Set<String> loadedMods) {
        return applyIf.get() && noExcludedModsPresent(loadedCoreMods, loadedMods)
                && allRequiredModsPresent(loadedCoreMods, loadedMods);
    }

    private boolean allRequiredModsPresent(Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (requiredMods == null) return true;
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < requiredMods.size(); i++) {
            ITargetedMod target = requiredMods.get(i);
            if (!isModPresent(target, loadedCoreMods, loadedMods)) return false;
        }
        return true;
    }

    private boolean noExcludedModsPresent(Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (excludedMods == null) return true;
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < excludedMods.size(); i++) {
            ITargetedMod target = excludedMods.get(i);
            if (isModPresent(target, loadedCoreMods, loadedMods)) return false;
        }
        return true;
    }

    private static boolean isModPresent(ITargetedMod target, Set<String> loadedCoreMods, Set<String> loadedMods) {
        // Check coremod first
        if (!loadedCoreMods.isEmpty() && target.getCoreModClass() != null
                && loadedCoreMods.contains(target.getCoreModClass())) {
            return true;
        }
        return !loadedMods.isEmpty() && target.getModId() != null && loadedMods.contains(target.getModId());
    }

    private void addMixinsForCurrentSide(List<String> mixinsToLoad, List<String> mixinsToNotLoad) {
        final boolean isClient = FMLLaunchHandler.side().isClient();
        if (commonMixins != null) mixinsToLoad.addAll(commonMixins);
        if (clientMixins != null) {
            if (isClient) mixinsToLoad.addAll(clientMixins);
            else mixinsToNotLoad.addAll(clientMixins);
        }
        if (serverMixins != null) {
            if (!isClient) mixinsToLoad.addAll(serverMixins);
            else mixinsToNotLoad.addAll(serverMixins);
        }
    }

    private void addAllMixinsTo(List<String> list) {
        if (commonMixins != null) list.addAll(commonMixins);
        if (clientMixins != null) list.addAll(clientMixins);
        if (serverMixins != null) list.addAll(serverMixins);
    }

}
