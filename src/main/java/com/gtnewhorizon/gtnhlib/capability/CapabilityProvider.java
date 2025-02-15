package com.gtnewhorizon.gtnhlib.capability;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// spotless:off
/**
 * Capability represents the ability of an object to expose a specific interface without directly implementing it,
 * allowing for a more flexible and decoupled approach compared to the traditional approach.
 * It is particularly useful when dealing with complex systems where objects may need to dynamically provide
 * different functionalities or when working with complex patterns, like delegating responsibilities to another class.
 * <p>
 * By implementing this interface, other systems are able to query and retrieve capabilities
 * without relying on direct interface implementations.
 * <p>
 * Note that it is required to replace all the {@code instanceof} checks with capability checks in order to
 * ensure compatibility when migrating from direct interface implementation to capability.
 * Avoid using capabilities for widely-used interfaces like {@code IFluidHandler} to prevent compatibility issues
 * with other mods.
 * <p>
 * Example usage:
 * <pre>{@code
 *     public class MyTileEntity implements CapabilityProvider {
 *         private MyCapability myCapability = new MyCapabilityImplementation();
 *
 *         @Override
 *         public <T> T getCapability(@NotNull Capability<T> capability, @NotNull ForgeDirection side) {
 *             if (capability == MyCapability.class) {
 *                 return capability.cast(myCapability);
 *             }
 *             return null;
 *         }
 *     }
 * }</pre>
 *
 * @see Capabilities
 */
// spotless:on
public interface CapabilityProvider {

    /**
     * Queries this provider for a capability implementation.
     * <p>
     * This method should:
     * <ul>
     * <li>Compare the requested capability with known capabilities using {@code ==} or do HashMap dispatch for a larger
     * quantity of supported capability types.</li>
     * <li>If matched, use {@link Class#cast} to return the implementation.</li>
     * <li>Return null if the capability is not supported.</li>
     * </ul>
     *
     * @param capability The capability being requested.
     * @param side       The {@link ForgeDirection} from which the capability is requested. Can be
     *                   {@link ForgeDirection#UNKNOWN UNKNOWN} if the direction is not relevant.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    @Nullable
    <T> T getCapability(@NotNull Class<? extends T> capability, @NotNull ForgeDirection side);
}
