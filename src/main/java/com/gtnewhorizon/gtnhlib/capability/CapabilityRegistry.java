package com.gtnewhorizon.gtnhlib.capability;

import java.util.HashMap;
import java.util.Map;

// spotless:off
/**
 * Registry for managing {@link Capability} instances. Each capability interface can have only one
 * capability instance associated with it.
 * <p>
 * Example usage:
 * <pre>{@code
 *     // Create and register a new capability
 *     public static final Capability<MyInterface> MY_CAPABILITY =
 *         CapabilityRegistry.INSTANCE.create(MyInterface.class);
 *
 *     // Get an existing capability
 *     Capability<MyInterface> capability = CapabilityRegistry.INSTANCE.get(MyInterface.class);
 * }</pre>
 */
// spotless:on
public final class CapabilityRegistry {

    /**
     * The singleton instance of the {@link CapabilityRegistry}.
     */
    public static final CapabilityRegistry INSTANCE = new CapabilityRegistry();

    /**
     * Creates a new {@link Capability} instance for the given capability interface.
     *
     * @param typeClass The {@link Class} object representing the capability interface.
     * @param <T>       The type of the capability interface.
     * @return The newly created {@link Capability} instance.
     * @throws RuntimeException if a capability with the same interface already exists.
     */
    public <T> Capability<T> create(Class<T> typeClass) {
        if (capabilities.get(typeClass) != null) {
            throw new RuntimeException(
                    String.format("Attempted to create new capability with existing class: %s", typeClass));
        }
        Capability<T> capability = new Capability<>(typeClass);
        capabilities.put(typeClass, capability);
        return capability;
    }

    /**
     * Retrieves the {@link Capability} instance associated with the given capability interface.
     *
     * @param typeClass The {@link Class} object representing the capability interface.
     * @param <T>       The type of the capability interface.
     * @return The {@link Capability} instance associated with the given interface.
     * @throws RuntimeException if no capability is registered for the given interface.
     */
    @SuppressWarnings("unchecked")
    public <T> Capability<T> get(Class<T> typeClass) {
        if (capabilities.get(typeClass) == null) {
            throw new RuntimeException(String.format("Attempted to get capability which doesn't exist: %s", typeClass));
        }
        return (Capability<T>) capabilities.get(typeClass);
    }

    /**
     * A map that stores the registered {@link Capability} instances, using the capability interface as the key.
     */
    private final Map<Class<?>, Capability<?>> capabilities = new HashMap<>();

    private CapabilityRegistry() {}
}
