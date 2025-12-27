package com.gtnewhorizon.gtnhlib.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ChatComponentCustomRegistry {

    private static final Map<String, Supplier<? extends AbstractChatComponentCustom>> CHAT_COMPONENT_REGISTRY = new ConcurrentHashMap<>();

    private ChatComponentCustomRegistry() {}

    /**
     * Register a custom chat component using its no-arg constructor call. Your class MUST have this, or it will not
     * compile.
     */
    public static void register(Supplier<? extends AbstractChatComponentCustom> factory) {
        AbstractChatComponentCustom instance = factory.get();

        String id = instance.getID();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Chat component ID must be non-null and non-empty.");
        }

        if (CHAT_COMPONENT_REGISTRY.putIfAbsent(id, factory) != null) {
            throw new IllegalArgumentException("Chat component with id '" + id + "' is already registered.");
        }
    }

    /**
     * Create a new instance of a registered custom chat component.
     */
    public static AbstractChatComponentCustom get(String id) {
        Supplier<? extends AbstractChatComponentCustom> factory = CHAT_COMPONENT_REGISTRY.get(id);
        if (factory == null) {
            throw new IllegalArgumentException(
                    String.format("Attempted to get '%s' from chat component registry, but it is not registered.", id));
        }

        return factory.get();
    }
}
