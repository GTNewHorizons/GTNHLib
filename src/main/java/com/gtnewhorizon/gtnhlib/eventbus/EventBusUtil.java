package com.gtnewhorizon.gtnhlib.eventbus;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gtnewhorizon.gtnhlib.mixins.early.fml.EnumHolderAccessor;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;

public final class EventBusUtil {

    public static final Boolean DEBUG_EVENT_BUS = Boolean.getBoolean("gtnhlib.debug.eventbus");
    private static final String CURRENT_SIDE = FMLLaunchHandler.side().name();

    @Getter
    private static final ObjectSet<String> classesToVisit = new ObjectOpenHashSet<>();
    @Getter
    private static final Object2ObjectMap<String, ObjectSet<MethodInfo>> methodsToSubscribe = new Object2ObjectOpenHashMap<>();
    @Getter
    private static final Object2ObjectMap<String, String> conditionsToCheck = new Object2ObjectOpenHashMap<>();
    @Getter
    private static final ObjectList<String> invalidMethods = new ObjectArrayList<>();

    static String getParameterClassInternal(String desc) {
        return desc.substring(desc.indexOf("(") + 2, desc.indexOf(";"));
    }

    static String getParameterClassName(String desc) {
        return getParameterClassInternal(desc).replace("/", ".");
    }

    static String getSimpleClassName(String desc) {
        return desc.substring(desc.lastIndexOf(".") + 1);
    }

    public static void harvestData(ASMDataTable table) {
        Set<ASMDataTable.ASMData> asmData = table.getAll(EventBusSubscriber.class.getName());
        ObjectSet<String> excludedClasses = getExcludedClasses(table);;
        for (ASMDataTable.ASMData data : asmData) {
            Map<String, Object> info = data.getAnnotationInfo();
            String className = data.getClassName();
            Phase phase = Phase.INIT;
            if (info != null) {
                if (info.containsKey("phase")) {
                    phase = Phase.valueOf(((EnumHolderAccessor) info.get("phase")).getValue());
                }
                // FML can't handle annotation enum arrays so this key is always null and only holds one value.
                if (info.containsKey(null)) {
                    String side = ((EnumHolderAccessor) info.get(null)).getValue();
                    if (!isValidSide(side, className)) continue;
                }
            }

            ModContainer mod = data.getCandidate().getContainedMods().get(0);
            if (excludedClasses.contains(className)) continue;

            phase.getModClassesForPhase().computeIfAbsent(mod, k -> new ObjectOpenHashSet<>()).add(className);
            classesToVisit.add(className);
        }
    }

    private static ObjectSet<String> getExcludedClasses(ASMDataTable dataTable) {
        // Due to the way we are registering events, we need to filter invalid sides out manually.
        // It's much faster to do it here than to load an invalid class and throw a couple exceptions.
        // For some reason it is MUCH faster to do getAll rather than getAnnotationsFor
        Set<ASMDataTable.ASMData> dat = dataTable.getAll(SideOnly.class.getName());
        ObjectSet<String> excludedClasses = new ObjectOpenHashSet<>();
        for (ASMDataTable.ASMData data : dat) {
            String className = data.getClassName();
            if (!data.getObjectName().equals(className)) {
                continue;
            }

            Map<String, Object> sideInfo = data.getAnnotationInfo();
            String side = ((EnumHolderAccessor) sideInfo.get("value")).getValue();
            if (!CURRENT_SIDE.equals(side)) {
                excludedClasses.add(className);
            }
        }
        return excludedClasses;
    }

    private static boolean isValidSide(String side, String className) {
        if (CURRENT_SIDE.equals("CLIENT")) {
            return side.equals(CURRENT_SIDE);
        }

        return !StringUtils.containsIgnoreCase(className, "client") && !side.equals("CLIENT");
    }
}
