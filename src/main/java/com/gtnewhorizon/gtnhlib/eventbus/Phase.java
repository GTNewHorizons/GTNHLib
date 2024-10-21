package com.gtnewhorizon.gtnhlib.eventbus;

import cpw.mods.fml.common.ModContainer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.AccessLevel;
import lombok.Getter;

public enum Phase {

    /**
     * Registered during {@link cpw.mods.fml.common.event.FMLConstructionEvent}.
     */
    CONSTRUCT,

    /**
     * Registered during {@link cpw.mods.fml.common.event.FMLPreInitializationEvent}.
     */
    PRE,

    /**
     * Registered during {@link cpw.mods.fml.common.event.FMLInitializationEvent}.
     */
    INIT;

    boolean hasExecuted = false;

    @Getter(AccessLevel.PACKAGE)
    private final Object2ObjectMap<ModContainer, ObjectSet<String>> modClassesForPhase = new Object2ObjectOpenHashMap<>();

}
