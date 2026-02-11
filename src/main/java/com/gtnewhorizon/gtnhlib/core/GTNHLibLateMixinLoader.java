package com.gtnewhorizon.gtnhlib.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.mixins.Mixins;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

@LateMixin
public class GTNHLibLateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.gtnhlib.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        mixins.add("MixinEnhancedInfusionRecipe");
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
