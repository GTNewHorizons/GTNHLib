package com.gtnewhorizon.gtnhlib.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import com.gtnewhorizon.gtnhlib.mixin.IMixins.Phase;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

@SuppressWarnings({ "unused", "ForLoopReplaceableByForEach" })
public class MixinBuilder {

    private @Nullable List<String> commonMixins;
    private @Nullable List<String> clientMixins;
    private @Nullable List<String> serverMixins;
    private @Nullable List<ITargetedMod> requiredMods;
    private @Nullable List<ITargetedMod> excludedMods;
    private @Nullable Phase phase;
    private Supplier<Boolean> applyIf = () -> true;

    public MixinBuilder() {}

    public MixinBuilder(String description) {}

    public MixinBuilder(IMixins iMixin) {
        commonMixins = iMixin.getCommonMixins();
        clientMixins = iMixin.getClientMixins();
        serverMixins = iMixin.getServerMixins();
        requiredMods = iMixin.getRequiredMods();
        excludedMods = iMixin.getExcludedMods();
        phase = iMixin.getPhase();
        applyIf = iMixin.getApplyIf();
    }

    /**
     * Add mixin classes that should be loaded both on the client and server
     */
    public MixinBuilder addCommonMixins(@Nonnull String... mixins) {
        if (commonMixins == null) commonMixins = new ArrayList<>(4);
        Collections.addAll(commonMixins, mixins);
        return this;
    }

    /**
     * Add mixin classes that should only be loaded on the client
     */
    public MixinBuilder addClientMixins(@Nonnull String... mixins) {
        if (clientMixins == null) clientMixins = new ArrayList<>(4);
        Collections.addAll(clientMixins, mixins);
        return this;
    }

    /**
     * Add mixin classes that should only be loaded on the dedicated server
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

    public MixinBuilder setApplyIf(@Nullable Supplier<Boolean> applyIf) {
        this.applyIf = applyIf;
        return this;
    }

    /**
     * Specify mods that are required to be present for this mixin to load
     */
    public MixinBuilder addRequiredMod(@Nonnull ITargetedMod mod) {
        if (requiredMods == null) requiredMods = new ArrayList<>(2);
        requiredMods.add(mod);
        return this;
    }

    /**
     * Specify mods that will disable this mixin if they are present
     */
    public MixinBuilder addExcludedMod(@Nonnull ITargetedMod mod) {
        if (excludedMods == null) excludedMods = new ArrayList<>(2);
        excludedMods.add(mod);
        return this;
    }

    private void validateBuilder(Enum<?> mixin) {
        int count = 0;
        if (commonMixins != null) count += commonMixins.size();
        if (clientMixins != null) count += clientMixins.size();
        if (serverMixins != null) count += serverMixins.size();
        if (count == 0) {
            throw new RuntimeException("No mixin class registered for IMixins : " + mixin.name());
        }
        if (requiredMods != null) {
            for (int i = 0; i < requiredMods.size(); i++) {
                validateTargetedMod(requiredMods.get(i), mixin);
            }
        }
        if (excludedMods != null) {
            for (int i = 0; i < excludedMods.size(); i++) {
                validateTargetedMod(excludedMods.get(i), mixin);
            }
        }
    }

    private void validateTargetedMod(ITargetedMod target, Enum<?> entry) {
        if (target.getModId() == null && target.getCoreModClass() == null
                && target.getTargetClass() == null
                && target.getClassNodeTest() == null
                && target.getJarNameTest() == null) {
            throw new RuntimeException(
                    "No information at all provided by ITargetedMod used by IMixins : " + entry.name());
        }
        if (phase == Phase.EARLY) {
            if (target.getCoreModClass() == null && target.getTargetClass() == null
                    && target.getClassNodeTest() == null
                    && target.getJarNameTest() == null) {
                throw new RuntimeException(
                        "Not enough information provided by ITargetedMod used by early IMixins : " + entry.name());
            }
        } else if (phase == Phase.LATE) {
            if (target.getModId() == null && target.getTargetClass() == null
                    && target.getClassNodeTest() == null
                    && target.getJarNameTest() == null) {
                throw new RuntimeException(
                        "Not enough information provided by ITargetedMod used by late IMixins : " + entry.name());
            }
        }
        if (target.getClassNodeTest() != null && target.getTargetClass() == null) {
            throw new RuntimeException(
                    "ITargetedMod used by IMixins : " + entry.name()
                            + " uses a ClassNode test but doesn't specify the target class");
        }
    }

    protected void loadMixins(Enum<?> mixin, List<String> mixinsToLoad, List<String> mixinsToNotLoad) {
        validateBuilder(mixin);
        if (shouldLoadMixin(Collections.emptySet(), Collections.emptySet())) {
            addMixinsForCurrentSide(mixinsToLoad, mixinsToNotLoad);
        } else {
            addAllMixinsTo(mixinsToNotLoad);
        }
    }

    protected void loadEarlyMixins(Enum<?> mixin, Set<String> loadedCoreMods, List<String> mixinsToLoad,
            List<String> mixinsToNotLoad) {
        if (phase != Phase.EARLY) return;
        validateBuilder(mixin);
        if (shouldLoadMixin(loadedCoreMods, Collections.emptySet())) {
            addMixinsForCurrentSide(mixinsToLoad, mixinsToNotLoad);
        } else {
            addAllMixinsTo(mixinsToNotLoad);
        }
    }

    protected void loadLateMixins(Enum<?> mixin, Set<String> loadedMods, List<String> mixinsToLoad,
            List<String> mixinsToNotLoad) {
        if (phase != Phase.LATE) return;
        validateBuilder(mixin);
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
        for (int i = 0; i < requiredMods.size(); i++) {
            ITargetedMod target = requiredMods.get(i);
            if (!isModPresent(target, loadedCoreMods, loadedMods)) return false;
        }
        return true;
    }

    private boolean noExcludedModsPresent(Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (excludedMods == null) return true;
        for (int i = 0; i < excludedMods.size(); i++) {
            ITargetedMod target = excludedMods.get(i);
            if (isModPresent(target, loadedCoreMods, loadedMods)) return false;
        }
        return true;
    }

    private static boolean isModPresent(ITargetedMod target, Set<String> loadedCoreMods, Set<String> loadedMods) {
        // 1. check coremod class
        if (!loadedCoreMods.isEmpty() && target.getCoreModClass() != null
                && loadedCoreMods.contains(target.getCoreModClass())) {
            return true;
        }
        // 2. check modID
        if (!loadedMods.isEmpty() && target.getModId() != null && loadedMods.contains(target.getModId())) {
            return true;
        }
        // 3. check class
        if (target.getTargetClass() != null) {
            try {
                ClassNode classNode = MixinService.getService().getBytecodeProvider()
                        .getClassNode(target.getTargetClass(), false);
                if (target.getClassNodeTest() == null) {
                    return true;
                } else {
                    // 4. test bytecode of target class
                    return target.getClassNodeTest().test(classNode);
                }
            } catch (ClassNotFoundException | IOException ignored) {}
        }
        // 5 find jar files and test jar name
        if (target.getJarNameTest() != null) {
            // TODO
        }
        return false;
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
