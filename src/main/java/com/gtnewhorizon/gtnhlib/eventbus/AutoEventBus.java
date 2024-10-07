package com.gtnewhorizon.gtnhlib.eventbus;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.SetMultimap;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.mixins.early.fml.EventBusAccessor;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AutoEventBus {

    private static final Boolean DEBUG = Boolean.getBoolean("gtnhlib.debug.eventbus");
    private static final Logger LOGGER = LogManager.getLogger("GTNHLib EventBus");
    private static final DummyEvent INVALID_EVENT = new DummyEvent();
    private static final ObjectSet<String> registeredClasses = new ObjectOpenHashSet<>();
    private static final Object2ObjectMap<String, ModContainer> classPathToModLookup;
    private static final Object2ObjectMap<Class<?>, Event> eventCache = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<String, Class<?>> eventClassCache = new Object2ObjectOpenHashMap<>();
    private static final Object2IntMap<Class<?>> eventsRegistered = new Object2IntOpenHashMap<>();
    private static boolean hasRegistered;
    private static ASMDataTable asmDataTable;

    static {
        classPathToModLookup = getModContainerPackageMap();
    }

    private enum EventBusType {

        FORGE(MinecraftForge.EVENT_BUS, AutoEventBus::isForgeEvent),
        OREGEN(MinecraftForge.ORE_GEN_BUS, AutoEventBus::isOreGenEvent),
        TERRAIN_GEN(MinecraftForge.TERRAIN_GEN_BUS, AutoEventBus::isTerrainEvent),
        FML(FMLCommonHandler.instance().bus(), AutoEventBus::isFMLEvent);

        private static final EventBusType[] VALUES = values();
        private final ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners;
        private final Map<Object, ModContainer> listenerOwners;
        private final Predicate<Class<?>> canRegister;
        private final int busID;

        EventBusType(EventBus instance, Predicate<Class<?>> canRegister) {
            this.canRegister = canRegister;
            EventBusAccessor accessor = (EventBusAccessor) instance;
            this.listeners = accessor.getListeners();
            this.listenerOwners = accessor.getListenerOwners();
            this.busID = accessor.getBusID();
        }

        private boolean canRegister(Class<?> clazz) {
            return canRegister.test(clazz);
        }

        private boolean isRegistered(Class<?> clazz) {
            return listeners.containsKey(clazz);
        }
    }

    public static void registerSubscribers() {
        if (hasRegistered) return;
        hasRegistered = true;
        for (ModContainer mod : Loader.instance().getModList()) {
            SetMultimap<String, ASMDataTable.ASMData> annotations = asmDataTable.getAnnotationsFor(mod);
            if (annotations == null) continue;
            Set<ASMDataTable.ASMData> subscribers = getOwningModAnnotation(annotations, mod, EventBusSubscriber.class);
            if (subscribers.isEmpty()) continue;
            Set<ASMDataTable.ASMData> conditionAnnotations = getOwningModAnnotation(
                    annotations,
                    mod,
                    EventBusSubscriber.Condition.class);
            Object2ObjectMap<String, String> conditions = null;
            if (!conditionAnnotations.isEmpty()) {
                conditions = new Object2ObjectOpenHashMap<>();
                for (ASMDataTable.ASMData data : conditionAnnotations) {
                    conditions.put(data.getClassName(), data.getObjectName());
                }
            }

            Object2ObjectMap<String, ObjectSet<String>> methods = getMethodsForMod(annotations, mod);
            for (ASMDataTable.ASMData data : subscribers) {
                try {
                    Class<?> clazz = Class.forName(data.getClassName(), false, Loader.instance().getModClassLoader());

                    if (registeredClasses.contains(clazz.getName())) {
                        if (DEBUG) {
                            LOGGER.info("Skipping registration for {}, already registered", clazz.getSimpleName());
                        }
                        continue;
                    }

                    if (!isValidSide(clazz)) {
                        if (DEBUG) {
                            LOGGER.info(
                                    "Skipping registration for {}, invalid side {}",
                                    clazz.getSimpleName(),
                                    FMLCommonHandler.instance().getSide());
                        }
                        continue;
                    }

                    if (conditions != null) {
                        if (!isConditionMet(clazz, conditions.get(data.getClassName()))) {
                            if (DEBUG) {
                                LOGGER.info("Skipping registration for {}, condition not met", clazz.getSimpleName());
                            }
                            continue;
                        }
                    }
                    ObjectSet<Method> methodsForClass = getMethodsToSubscribe(clazz, methods.get(data.getClassName()));
                    register(clazz, mod, methodsForClass);
                } catch (ClassNotFoundException | IllegalAccessException e) {
                    if (DEBUG) LOGGER.error("Failed to load class for annotation", e);
                }
            }
        }
        if (DEBUG) {
            printFailedEvents();
            printRegisteredEvents();
        }
    }

    private static void register(Class<?> target, ModContainer classOwner, ObjectSet<Method> methods) {
        Set<Class<?>> registeredEvents = new HashSet<>();
        for (Method method : methods) {
            Class<?> eventType = method.getParameterTypes()[0];

            if (registerEvent(eventType, target, method, classOwner)) {
                if (DEBUG) {
                    LOGGER.info(
                            "Registered event handler for {} in class {} with owner {}",
                            eventType.getName(),
                            target.getName(),
                            classOwner.getModId());
                }
                registeredEvents.add(eventType);
            }
        }

        registerOwner(target, classOwner, registeredEvents);
    }

    private static boolean registerEvent(Class<?> eventClass, Class<?> target, Method method, ModContainer owner) {
        try {
            Event event = getCachedEvent(eventClass);
            if (INVALID_EVENT.equals(event)) return false;

            StaticASMEventHandler listener = new StaticASMEventHandler(target, method, owner);
            boolean registered = false;
            for (EventBusType bus : EventBusType.VALUES) {
                if (bus.isRegistered(eventClass) || !bus.canRegister(eventClass)) {
                    continue;
                }
                event.getListenerList().register(bus.busID, listener.getPriority(), listener);
                bus.listeners.computeIfAbsent(target, k -> new ArrayList<>()).add(listener);
                registered = true;

                if(DEBUG) {
                    eventsRegistered.merge(eventClass, 1, Integer::sum);
                }
            }

            return registered;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void registerOwner(Class<?> target, ModContainer owner, Set<Class<?>> registeredEvents) {
        for (EventBusType bus : EventBusType.VALUES) {
            for (Class<?> event : registeredEvents) {
                if (bus.canRegister(event)) {
                    registeredClasses.add(target.getName());
                    bus.listenerOwners.put(target, owner);
                    break;
                }
            }
        }
    }

    private static @Nonnull Event getCachedEvent(Class<?> eventClass) {
        return eventCache.computeIfAbsent(eventClass, e -> {
            try {
                return (Event) ConstructorUtils.invokeConstructor(eventClass);
            } catch (NoClassDefFoundError | ExceptionInInitializerError | Exception ex) {
                return INVALID_EVENT;
            }
        });
    }

    private static boolean isConditionMet(@NotNull Class<?> clazz, @Nullable String condition) {
        if (condition == null) return true;
        try {
            if (condition.contains("()Z")) {
                Method method = clazz.getDeclaredMethod(condition.substring(0, condition.indexOf("(")));
                method.setAccessible(true);
                return (boolean) method.invoke(null);
            }

            Field field = clazz.getDeclaredField(condition);
            field.setAccessible(true);
            return field.getBoolean(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            if (DEBUG) LOGGER.error("Failed to invoke condition {} for class {}", condition, clazz, e);
            return false;
        }
    }

    private static ObjectSet<Method> getMethodsToSubscribe(Class<?> clazz, ObjectSet<String> methodDescs) {
        if (methodDescs.isEmpty()) return ObjectSets.emptySet();
        ObjectSet<Method> methodsToSub = new ObjectOpenHashSet<>();
        for (String methodDesc : methodDescs) {
            Class<?> eventClass = getCachedEventClass(methodDesc);
            if (INVALID_EVENT.getClass().equals(eventClass)) continue;
            String methodName = methodDesc.substring(0, methodDesc.indexOf("("));
            try {
                Method method = clazz.getMethod(methodName, eventClass);
                if (!Modifier.isStatic(method.getModifiers()) || !method.isAnnotationPresent(SubscribeEvent.class)) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0])) {
                    throw new IllegalArgumentException(
                            "Method " + method
                                    + " has @SubscribeEvent annotation, but requires invalid parameters. "
                                    + "Event handler methods must take a single parameter of type Event.");
                }
                methodsToSub.add(method);
            } catch (NoSuchMethodException ignored) {
                if (DEBUG) {
                    LOGGER.error("Failed to register method {} for class {}", methodName, clazz);
                }
            }
            return methodsToSub;
        }
        return ObjectSets.emptySet();
    }

    private static Object2ObjectMap<String, ObjectSet<String>> getMethodsForMod(
            SetMultimap<String, ASMDataTable.ASMData> annotationTable, ModContainer mod) {
        Set<ASMDataTable.ASMData> methodAnnotations = getOwningModAnnotation(
                annotationTable,
                mod,
                SubscribeEvent.class);
        if (methodAnnotations.isEmpty()) return Object2ObjectMaps.emptyMap();

        Object2ObjectMap<String, ObjectSet<String>> methods = new Object2ObjectOpenHashMap<>();
        for (ASMDataTable.ASMData data : methodAnnotations) {
            methods.computeIfAbsent(data.getClassName(), k -> new ObjectOpenHashSet<>()).add(data.getObjectName());
        }
        return methods;
    }

    private static @Nonnull Class<?> getCachedEventClass(String methodDesc) {
        String className = methodDesc.substring(methodDesc.indexOf("(L") + 2, methodDesc.indexOf(";)"))
                .replaceAll("/", ".");
        return eventClassCache.computeIfAbsent(className, a -> {
            try {
                return Class.forName(className);
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                if (DEBUG) {
                    LOGGER.error("Failed to register method {} for class {}", methodDesc, className, e);
                }
                return INVALID_EVENT.getClass();
            }
        });
    }

    private static Object2ObjectMap<String, ModContainer> getModContainerPackageMap() {
        Object2ObjectMap<String, ModContainer> classToModContainer = new Object2ObjectOpenHashMap<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            Object modObject = container.getMod();
            if (modObject == null) continue;
            Package modPackage = modObject.getClass().getPackage();
            if (modPackage == null) continue;
            classToModContainer.put(modPackage.getName(), container);
        }
        return classToModContainer;
    }

    private static @Nonnull ModContainer getOwningModContainer(String className) {
        return classPathToModLookup.object2ObjectEntrySet().stream().filter(e -> className.startsWith(e.getKey()))
            .map(Map.Entry::getValue).findFirst().orElse(Loader.instance().getMinecraftModContainer());
    }

    private static @Nonnull Set<ASMDataTable.ASMData> getOwningModAnnotation(
        SetMultimap<String, ASMDataTable.ASMData> dataTable, ModContainer mod, Class<?> annotationClass) {
        Set<ASMDataTable.ASMData> annotationData = dataTable.get(annotationClass.getName());
        if (annotationData == null || annotationData.isEmpty()) return Collections.emptySet();
        if (dataTable.get(Mod.class.getName()).size() > 1) {
            annotationData = annotationData.stream()
                .filter(data -> Objects.equals(getOwningModContainer(data.getClassName()), mod))
                .collect(Collectors.toSet());
        }
        return annotationData;
    }

    private static boolean isValidSide(Class<?> subscribedClass) throws IllegalAccessException {
        Side currentSide = FMLCommonHandler.instance().getSide();
        if (currentSide.isClient()) return true;

        EventBusSubscriber subscriber = subscribedClass.getAnnotation(EventBusSubscriber.class);
        Side[] sides = subscriber.side();
        if (sides.length == 1) {
            return currentSide == sides[0];
        }

        return !StringUtils.containsIgnoreCase(subscribedClass.getName(), "client");
    }

    public static void setDataTable(ASMDataTable dataTable) {
        if (!Loader.instance().activeModContainer().getModId().equals(GTNHLib.MODID)) {
            return;
        }
        asmDataTable = dataTable;
    }

    private static void printFailedEvents() {
        Side side = FMLCommonHandler.instance().getSide();
        for (Map.Entry<Class<?>, Event> entry : eventCache.object2ObjectEntrySet()) {
            if (entry.getValue() == null) {
                LOGGER.error("Failed to register event {} for side {}", entry.getKey(), side);
            }
        }
    }

    private static void printRegisteredEvents() {
        for (Object2IntMap.Entry<Class<?>> entry : eventsRegistered.object2IntEntrySet()) {
            LOGGER.info("Event {} was registered {} times", entry.getKey().getSimpleName(), entry.getIntValue());
        }
    }

    private static boolean isFMLEvent(Class<?> event) {
        return event.getName().startsWith("cpw.mods.fml");
    }

    private static boolean isTerrainEvent(Class<?> event) {
        return event.getName().startsWith("net.minecraftforge.event.terraingen") && !isOreGenEvent(event);
    }

    private static boolean isOreGenEvent(Class<?> event) {
        return OreGenEvent.class.isAssignableFrom(event);
    }

    private static boolean isForgeEvent(Class<?> event) {
        return !isFMLEvent(event) && !isTerrainEvent(event) && !isOreGenEvent(event);
    }

    private static class DummyEvent extends Event {}
}
