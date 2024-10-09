package com.gtnewhorizon.gtnhlib.eventbus;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;

public final class EventBusUtil {

    public static final Boolean DEBUG_EVENT_BUS = Boolean.getBoolean("gtnhlib.debug.eventbus");

    @Getter
    private static final ObjectSet<String> classesToVisit = new ObjectOpenHashSet<>();
    @Getter
    private static final Object2ObjectMap<String, ObjectSet<MethodInfo>> methodsToSubscribe = new Object2ObjectOpenHashMap<>();
    @Getter
    private static final Object2ObjectMap<String, String> conditionsToCheck = new Object2ObjectOpenHashMap<>();

    static String getParameterClassInternal(String desc) {
        return desc.substring(desc.indexOf("(") + 2, desc.indexOf(";"));
    }

    static String getParameterClassName(String desc) {
        return getParameterClassInternal(desc).replace("/", ".");
    }

    static String getSimpleClassName(String desc) {
        return desc.substring(desc.lastIndexOf(".") + 1);
    }
}
