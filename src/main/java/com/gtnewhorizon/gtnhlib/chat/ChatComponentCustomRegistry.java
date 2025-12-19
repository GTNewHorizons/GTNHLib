package com.gtnewhorizon.gtnhlib.chat;

import java.util.HashMap;
import java.util.Map;

public final class ChatComponentCustomRegistry {

    private static final Map<String, Class<? extends IChatComponentCustomSerializer>> CHAT_COMPONENT_REGISTRY = new HashMap<>();

    public static void register(Class<? extends IChatComponentCustomSerializer> chatComponent) {
        try {
            IChatComponentCustomSerializer customSerializer = chatComponent.getDeclaredConstructor().newInstance();

            if (CHAT_COMPONENT_REGISTRY.containsKey(customSerializer.getID())) {
                throw new IllegalArgumentException(
                        "Chat component " + chatComponent.getName() + " is already registered.");
            }

            CHAT_COMPONENT_REGISTRY.put(customSerializer.getID(), chatComponent);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Registered chat component " + chatComponent.getSimpleName()
                            + " must have a no-arg public constructor.",
                    e);
        }
    }

    public static IChatComponentCustomSerializer get(String id) {
        Class<? extends IChatComponentCustomSerializer> clazz = CHAT_COMPONENT_REGISTRY.get(id);
        if (clazz == null) {
            throw new IllegalArgumentException(
                    String.format("Attempted to get '%s' from chat component registry, but it is not registered.", id));
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    String.format(
                            "Failed to instantiate chat component '%s'. It must have a public no-arg constructor.",
                            id),
                    e);
        }
    }
}
