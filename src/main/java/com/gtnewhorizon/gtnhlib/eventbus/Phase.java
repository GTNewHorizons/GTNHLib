package com.gtnewhorizon.gtnhlib.eventbus;

import cpw.mods.fml.common.ModContainer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;

public enum Phase {

    CONSTRUCT,
    PRE,
    INIT;

    boolean hasExecuted = false;

    @Getter
    private final Object2ObjectMap<ModContainer, ObjectSet<String>> modClassesForPhase = new Object2ObjectOpenHashMap<>();

}
