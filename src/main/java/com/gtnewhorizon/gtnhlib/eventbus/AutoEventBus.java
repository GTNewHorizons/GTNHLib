package com.gtnewhorizon.gtnhlib.eventbus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.SetMultimap;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.reflect.Fields;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AutoEventBus {

    private static final Boolean DEBUG = Boolean.getBoolean("gtnhlib.debug.eventbus");
    private static final Logger LOGGER = LogManager.getLogger("GTNHLib EventBus");
    private static final Object2BooleanMap<String> validEventsForSide = new Object2BooleanOpenHashMap<>();
    private static final ObjectSet<String> registeredClasses = new ObjectOpenHashSet<>();
    private static boolean hasRegistered;
    private static ASMDataTable asmDataTable;

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
            this.listeners = ReflectionHelper.getPrivateValue(EventBus.class, instance, "listeners");
            this.listenerOwners = ReflectionHelper.getPrivateValue(EventBus.class, instance, "listenerOwners");
            this.busID = ReflectionHelper.getPrivateValue(EventBus.class, instance, "busID");
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
        Fields.ofClass(EventBus.class).getField(Fields.LookupType.DECLARED, "listeners", ConcurrentHashMap.class);
        Object2ObjectMap<String, ModContainer> classToModLookup = getModContainerPackageMap();
        for (ModContainer mod : Loader.instance().getModList()) {
            SetMultimap<String, ASMDataTable.ASMData> annotations = asmDataTable.getAnnotationsFor(mod);
            if (annotations == null) continue;
            Set<ASMDataTable.ASMData> subscribers = annotations.get(EventBusSubscriber.class.getName());
            if (subscribers == null || subscribers.isEmpty()) continue;

            Set<ASMDataTable.ASMData> conditionAnnotations = annotations
                    .get(EventBusSubscriber.Condition.class.getName());

            if (annotations.get(Mod.class.getName()).size() > 1) {
                subscribers = subscribers.stream().filter(
                        data -> Objects.equals(getOwningModContainer(classToModLookup, data.getClassName()), mod))
                        .collect(Collectors.toSet());
                conditionAnnotations = conditionAnnotations.stream().filter(
                        data -> Objects.equals(getOwningModContainer(classToModLookup, data.getClassName()), mod))
                        .collect(Collectors.toSet());
            }

            Object2ObjectMap<String, String> conditions = null;
            if (!conditionAnnotations.isEmpty()) {
                conditions = new Object2ObjectOpenHashMap<>();
                for (ASMDataTable.ASMData data : conditionAnnotations) {
                    conditions.put(data.getClassName(), data.getObjectName());
                }
            }

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

                    register(clazz, mod);
                } catch (ClassNotFoundException | IllegalAccessException e) {
                    if (DEBUG) LOGGER.error("Failed to load class for annotation", e);
                }
            }
        }
    }

    private static void register(Class<?> target, ModContainer classOwner) {
        Set<Class<?>> registeredEvents = new HashSet<>();
        for (Method method : target.getMethods()) {
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

            Class<?> eventType = parameterTypes[0];
            if (!isEventSafeToRegister(eventType)) continue;

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
            Constructor<?> ctr = eventClass.getConstructor();
            ctr.setAccessible(true);
            Event event = (Event) ctr.newInstance();
            StaticASMEventHandler listener = new StaticASMEventHandler(target, method, owner);

            boolean registered = false;
            for (EventBusType bus : EventBusType.VALUES) {
                if (bus.isRegistered(event.getClass()) || !bus.canRegister(event.getClass())) {
                    continue;
                }
                event.getListenerList().register(bus.busID, listener.getPriority(), listener);
                bus.listeners.computeIfAbsent(target, k -> new ArrayList<>()).add(listener);
                registered = true;
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

    private static boolean isValidSide(Class<?> subscribedClass) throws IllegalAccessException {
        Side currentSide = FMLCommonHandler.instance().getSide();
        if (currentSide.isClient()) return true;

        EventBusSubscriber subscriber = subscribedClass.getAnnotation(EventBusSubscriber.class);
        Side[] sides = subscriber.side();
        if (sides.length == 1) {
            return currentSide == sides[0];
        }

        return !subscribedClass.getName().contains("client");
    }

    private static boolean isEventSafeToRegister(Class<?> eventClass) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            return true;
        }

        if (validEventsForSide.containsKey(eventClass.getName())) {
            return validEventsForSide.getBoolean(eventClass.getName());
        }

        try {
            // noinspection ResultOfMethodCallIgnored
            eventClass.getDeclaredFields();
            validEventsForSide.put(eventClass.getName(), true);
            return true;
        } catch (NoClassDefFoundError e) {
            validEventsForSide.put(eventClass.getName(), false);
            return false;
        }
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

    private static @Nonnull ModContainer getOwningModContainer(Object2ObjectMap<String, ModContainer> lookupMap,
            String className) {
        return lookupMap.object2ObjectEntrySet().stream().filter(e -> className.startsWith(e.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(Loader.instance().getMinecraftModContainer());
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

    public static void setDataTable(ASMDataTable dataTable) {
        if (!Loader.instance().activeModContainer().getModId().equals(GTNHLib.MODID)) {
            return;
        }
        asmDataTable = dataTable;
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
}
