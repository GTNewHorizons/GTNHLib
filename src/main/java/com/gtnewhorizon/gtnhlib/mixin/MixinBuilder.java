package com.gtnewhorizon.gtnhlib.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class MixinBuilder {

    public final List<String> mixinClasses = new ArrayList<>();
    public Supplier<Boolean> applyIf = () -> true;
    public Side side = Side.BOTH;
    public Phase phase = Phase.LATE;
    public final List<ITargetedMod> targetedMods = new ArrayList<>();
    public final List<ITargetedMod> excludedMods = new ArrayList<>();

    public MixinBuilder(@SuppressWarnings("unused") String description) {}

    public MixinBuilder addMixinClasses(String... mixinClasses) {
        this.mixinClasses.addAll(Arrays.asList(mixinClasses));
        return this;
    }

    public MixinBuilder setPhase(Phase phase) {
        this.phase = phase;
        return this;
    }

    public MixinBuilder setSide(Side side) {
        this.side = side;
        return this;
    }

    public MixinBuilder setApplyIf(Supplier<Boolean> applyIf) {
        this.applyIf = applyIf;
        return this;
    }

    public MixinBuilder addTargetedMod(ITargetedMod mod) {
        this.targetedMods.add(mod);
        return this;
    }

    public MixinBuilder addExcludedMod(ITargetedMod mod) {
        this.excludedMods.add(mod);
        return this;
    }

}
