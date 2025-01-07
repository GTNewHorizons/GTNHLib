package com.gtnewhorizon.gtnhlib.eventbus;

import static com.gtnewhorizon.gtnhlib.eventbus.EventBusUtil.DEBUG_EVENT_BUS;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.mixins.early.fml.EventBusAccessor;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AutoEventBus {

    private static final Logger LOGGER = LogManager.getLogger("GTNHLib EventBus");
    private static final DummyEvent INVALID_EVENT = new DummyEvent();
    private static final Object2ObjectMap<String, Event> eventCache = new Object2ObjectOpenHashMap<>();
    private static final Object2BooleanMap<String> optionalMods = new Object2BooleanOpenHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType CONDITION_TYPE = MethodType.methodType(boolean.class);

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
    }

    public static void executePhase(Phase phase) {
        if (phase.hasExecuted) return;
        phase.hasExecuted = true;
        Object2ObjectMap<ModContainer, ObjectSet<String>> subscribers = phase.getModClassesForPhase();
        if (subscribers.isEmpty()) return;

        for (Object2ObjectMap.Entry<ModContainer, ObjectSet<String>> entry : subscribers.object2ObjectEntrySet()) {
            for (String className : entry.getValue()) {
                try {
                    Class<?> clazz = Class.forName(className, false, Loader.instance().getModClassLoader());
                    String conditionToCheck = EventBusUtil.getConditionsToCheck().get(className);
                    if (conditionToCheck != null && !isConditionMet(clazz, conditionToCheck)) {
                        if (DEBUG_EVENT_BUS) {
                            LOGGER.info("Skipping registration for {}, condition not met", clazz.getSimpleName());
                        }
                        continue;
                    }

                    ObjectSet<MethodInfo> methods = EventBusUtil.getMethodsToSubscribe().get(className);
                    if (methods == null || methods.isEmpty()) continue;
                    register(entry.getKey(), clazz, methods);
                } catch (ClassNotFoundException e) {
                    if (DEBUG_EVENT_BUS) LOGGER.error("Failed to load class {}", className, e);
                }
            }
        }

        if (phase == Phase.INIT) {
            ObjectList<String> invalidMethods = EventBusUtil.getInvalidMethods();
            if (invalidMethods.size() == 1) {
                throw new IllegalArgumentException(invalidMethods.get(0));
            } else if (invalidMethods.size() > 1) {
                int i;
                for (i = 0; i < invalidMethods.size() - 1; i++) {
                    LOGGER.error(invalidMethods.get(i));
                }
                throw new IllegalArgumentException("Encountered" + i + "invalid methods. " + invalidMethods.get(i));
            }
        }
    }

    private static void register(ModContainer classOwner, Class<?> target, ObjectSet<MethodInfo> methods) {
        for (MethodInfo method : methods) {
            try {
                if (method.getOptionalMod() != null) {
                    if (!optionalMods
                            .computeIfAbsent(method.getOptionalMod(), (Predicate<String>) Loader::isModLoaded)) {
                        continue;
                    }
                }

                Event event = getCachedEvent(EventBusUtil.getParameterClassName(method.desc));
                if (INVALID_EVENT.equals(event)) continue;

                StaticASMEventHandler listener = new StaticASMEventHandler(method);
                for (EventBusType bus : EventBusType.VALUES) {
                    if (!bus.canRegister(event.getClass())) {
                        continue;
                    }
                    event.getListenerList().register(bus.busID, listener.getPriority(), listener);
                    bus.listenerOwners.putIfAbsent(target, classOwner);
                    bus.listeners.computeIfAbsent(target, k -> new ArrayList<>()).add(listener);

                    if (DEBUG_EVENT_BUS) {
                        LOGGER.info("Registered event handler for {} on {}", event.getClass().getSimpleName(), bus);
                    }
                }
            } catch (Exception e) {
                if (DEBUG_EVENT_BUS) LOGGER.error("Failed to register event handler for {}", method.desc, e);
            }
        }
    }

    private static @Nonnull Event getCachedEvent(String eventClass) {
        return eventCache.computeIfAbsent(eventClass, e -> {
            try {
                Class<?> clazz = Class.forName(eventClass, false, Loader.instance().getModClassLoader());
                return (Event) ConstructorUtils.invokeConstructor(clazz);
            } catch (NoClassDefFoundError | ExceptionInInitializerError | Exception ex) {
                // Event was likely for a mod that is not loaded or an invalid side.
                // The subscribed method will never be invoked, so we can safely ignore it.
                if (DEBUG_EVENT_BUS) LOGGER.error("Failed to create event instance for {}", eventClass, ex);
                return INVALID_EVENT;
            }
        });
    }

    private static boolean isConditionMet(@NotNull Class<?> clazz, @Nullable String condition) {
        if (condition == null) return true;
        try {
            MethodHandle handle = MethodHandles.publicLookup()
                    .findStatic(clazz, condition.substring(0, condition.indexOf("(")), CONDITION_TYPE);
            CallSite call = LambdaMetafactory.metafactory(
                    LOOKUP,
                    "getAsBoolean",
                    MethodType.methodType(BooleanSupplier.class),
                    CONDITION_TYPE,
                    handle,
                    CONDITION_TYPE);
            return ((BooleanSupplier) call.getTarget().invokeWithArguments()).getAsBoolean();
        } catch (Throwable e) {
            LOGGER.error("Failed to invoke condition {} for class {}", condition, clazz, e);
            return false;
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

    private static class DummyEvent extends Event {
    }
}
