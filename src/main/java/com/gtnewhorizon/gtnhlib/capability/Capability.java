package com.gtnewhorizon.gtnhlib.capability;

import java.util.Objects;

import lombok.Getter;

// spotless:off
/**
 * {@link Capability} represents the ability of an object to expose a specific interface,
 * allowing for a more flexible and decoupled approach compared to traditional interface implementations.
 * It is particularly useful when dealing with complex systems where objects may need to dynamically provide
 * different functionalities or when working with composite patterns.
 * <p>
 * Capabilities offer a way to avoid excessive boilerplate code associated with implementing multiple interfaces
 * and delegating method calls. Instead of directly implementing interfaces, objects can provide capabilities
 * that expose specific functionalities on demand.
 * <p>
 * Here are the key concepts in the capability system:
 * <ul>
 *     <li><b>Capability:</b> Represents a specific ability or functionality that an object can provide.</li>
 *     <li><b>Capability Provider:</b> An object that can provide one or more capabilities.</li>
 *     <li><b>Capability Interface:</b> The interface that defines the methods exposed by a capability.</li>
 *     <li><b>Capability Implementation:</b> A concrete implementation of a capability interface.</li>
 * </ul>
 * <p>
 * <b>Providing a Capability:</b>
 * <ol>
 *     <li>Implement the {@link ICapabilityProvider} interface in your class.</li>
 *     <li>
 *         Implement the {@link ICapabilityProvider#getCapability} method. Check the requested {@code capability}
 *         and return the appropriate capability implementation if available.
 *     </li>
 * </ol>
 * <pre>{@code
 *     class MyTileEntity implements ICapabilityProvider {
 *         private MyInterface delegate = new MyInterfaceImpl();
 *         public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable ForgeDirection side) {
 *             if (capability == MY_CAPABILITY) {
 *                 return MY_CAPABILITY.cast(delegate);
 *             }
 *             return null;
 *         }
 *     }
 * }</pre>
 * <p>
 * <b>Registering a Capability:</b>
 * <p>
 * Use {@link CapabilityRegistry} to register your capability.
 * <pre>{@code
 *     public static final Capability<MyInterface> MY_CAPABILITY = CapabilityRegistry.INSTANCE.create(MyInterface.class);
 * }</pre>
 * <p>
 * <b>Retrieving a Capability:</b>
 * <p>
 * Use {@link CapabilityUtil} to retrieve capabilities from objects like TileEntities, ItemStacks, or Entities.
 * This utility provides backward compatibility by checking if the object directly implements the interface.
 * <pre>{@code
 *     TileEntity neighbor = getNeighbor();
 *     MyInterface myInterface = CapabilityUtil.getCapability(neighbor, MY_CAPABILITY);
 *     if (myInterface != null) {
 *         // Use the capability
 *     }
 * }</pre>
 * <p>
 * <b>Important Considerations:</b>
 * <ul>
 *     <li>
 *         Replace all {@code instanceof} checks with capability checks to ensure proper compatibility.
 *     </li>
 *     <li>
 *         Avoid using capabilities for widely-used interfaces like {@code IFluidHandler} to prevent
 *         compatibility issues with other mods.
 *     </li>
 *     <li>
 *         Capabilities can return null. Always check for null before using a capability.
 *     </li>
 * </ul>
 *
 * @param <T> The type of the capability interface.
 */
// spotless:on
@Getter
public final class Capability<T> {

    /**
     * Casts the given object to the capability interface type {@code T}. This method is typically used when a
     * capability provider returns an object through {@link ICapabilityProvider#getCapability}.
     *
     * @param object The object to cast to the capability interface.
     * @param <R>    The type to cast the object to, inferred from the context.
     * @return The cast object, or null if the object is null.
     * @throws ClassCastException if the object cannot be cast to the capability interface.
     */
    @SuppressWarnings("unchecked")
    public <R> R cast(T object) {
        return (R) object;
    }

    private final Class<T> typeClass;

    Capability(Class<T> typeClass) {
        this.typeClass = typeClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Capability<?>that)) return false;
        return Objects.equals(typeClass, that.typeClass);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(typeClass);
    }
}
