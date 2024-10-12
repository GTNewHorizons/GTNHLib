package com.gtnewhorizon.gtnhlib.core.transformer;

import static com.gtnewhorizon.gtnhlib.eventbus.EventBusUtil.DEBUG_EVENT_BUS;

import java.util.Arrays;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusUtil;
import com.gtnewhorizon.gtnhlib.eventbus.MethodInfo;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class EventBusSubTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("GTNHLib|EventBusSubTransformer");
    private static final String OPTIONAL_DESC = Type.getDescriptor(Optional.Method.class);
    private static final String SIDEONLY_DESC = Type.getDescriptor(SideOnly.class);
    private static final String SUBSCRIBE_DESC = Type.getDescriptor(SubscribeEvent.class);
    private static final String CONDITION_DESC = Type.getDescriptor(EventBusSubscriber.Condition.class);
    private static final List<String> ANNOTATIONS = Arrays
            .asList(OPTIONAL_DESC, SIDEONLY_DESC, SUBSCRIBE_DESC, CONDITION_DESC);
    private static final String CURRENT_SIDE = FMLLaunchHandler.side().name();
    private static final ObjectSet<String> classesToVisit = EventBusUtil.getClassesToVisit();

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        // It's either too early or this class isn't an @EventBusSubscriber
        if (classesToVisit.isEmpty() || !classesToVisit.contains(transformedName)) {
            return basicClass;
        }

        final ClassReader cr = new ClassReader(basicClass);
        final ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);

        // Processing all of this from the ASMDataTable is way too slow
        for (MethodNode mn : cn.methods) {
            Object2ObjectMap<String, AnnotationNode> usableAnnotations = getUsableAnnotations(mn.visibleAnnotations);
            if (usableAnnotations.isEmpty()) continue;

            if (!matchesSide(usableAnnotations.get(SIDEONLY_DESC))) {
                if (DEBUG_EVENT_BUS) {
                    LOGGER.info("Skipping method {} due to side mismatch", transformedName);
                }
                continue;
            }

            AnnotationNode subscribe = usableAnnotations.get(SUBSCRIBE_DESC);
            boolean condition = usableAnnotations.containsKey(CONDITION_DESC);
            if ((mn.access & Opcodes.ACC_STATIC) == 0) {
                if (!condition && subscribe != null) {
                    EventBusUtil.getInvalidMethods().add(
                            "Encountered unexpected non-static method: " + transformedName + " " + mn.name + mn.desc);
                }
                continue;
            }

            if (condition) {
                if (mn.desc.equals("()Z")) {
                    EventBusUtil.getConditionsToCheck().put(transformedName, mn.name + mn.desc);
                } else {
                    EventBusUtil.getInvalidMethods().add(
                            "Invalid condition method: " + transformedName
                                    + " "
                                    + mn.name
                                    + mn.desc
                                    + ". Condition method must have no parameters and return a boolean.");
                }
                continue;
            }

            if (subscribe == null) {
                if (DEBUG_EVENT_BUS) {
                    LOGGER.info(
                            "Skipping method {} with annotations {}. No @SubscribeEvent found.",
                            transformedName,
                            usableAnnotations.keySet());
                }
                continue;
            }
            Object[] subscribeInfo = getSubscribeInfo(subscribe);
            MethodInfo methodInfo = new MethodInfo(
                    transformedName,
                    mn.name,
                    mn.desc,
                    (Boolean) subscribeInfo[0],
                    (EventPriority) subscribeInfo[1]);
            AnnotationNode optional = usableAnnotations.get(OPTIONAL_DESC);
            if (optional != null) {
                List<Object> values = optional.values;
                methodInfo.setOptionalMod((String) values.get(1));
                if (DEBUG_EVENT_BUS) {
                    LOGGER.info(
                            "Found optional mod {} for method {}",
                            methodInfo.getOptionalMod(),
                            methodInfo.getKey());
                }
            }

            EventBusUtil.getMethodsToSubscribe().computeIfAbsent(transformedName, k -> new ObjectOpenHashSet<>())
                    .add(methodInfo);
            if (DEBUG_EVENT_BUS) {
                LOGGER.info("Found subscribed method {}", methodInfo.getKey());
            }
        }

        return basicClass;
    }

    private static Object2ObjectMap<String, AnnotationNode> getUsableAnnotations(List<AnnotationNode> annotations) {
        if (annotations == null) return Object2ObjectMaps.emptyMap();
        Object2ObjectMap<String, AnnotationNode> usable = new Object2ObjectOpenHashMap<>();
        for (AnnotationNode ann : annotations) {
            if (ANNOTATIONS.contains(ann.desc)) {
                usable.put(ann.desc, ann);
            }
        }
        return usable;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean matchesSide(AnnotationNode side) {
        if (side == null) return true;
        for (int x = 0; x < side.values.size() - 1; x += 2) {
            Object key = side.values.get(x);
            Object value = side.values.get(x + 1);
            if (!(key instanceof String) || !key.equals("value")) continue;
            if (!(value instanceof String[]array)) continue;
            if (!array[1].equals(CURRENT_SIDE)) {
                return false;
            }
        }
        return true;
    }

    private static Object[] getSubscribeInfo(AnnotationNode annotation) {
        Object[] info = { false, EventPriority.NORMAL };
        if (annotation.values == null) return info;
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            Object key = annotation.values.get(i);
            Object value = annotation.values.get(i + 1);
            if (!(key instanceof String)) continue;
            if (key.equals("receiveCanceled")) {
                info[0] = value;
            } else if (key.equals("priority") && value instanceof String[]array) {
                info[1] = EventPriority.valueOf(array[1]);
            }
        }
        return info;
    }
}
