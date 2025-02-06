package com.gtnewhorizon.gtnhlib.capability;

import net.minecraftforge.common.util.ForgeDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// spotless:off
/**
 * By implementing this interface, classes indicate that they can offer certain {@link Capability Capabilities}.
 * Implementing this interface allows other systems to query and retrieve capabilities without relying on direct
 * interface implementations. This can be useful for extending or overriding behavior at runtime.
 * Typically implemented by TileEntities, Items or Entities.
 * <p>
 * Example usage:
 * <pre>{@code
 *     public class MyTileEntity implements ICapabilityProvider {
 *         private MyCapabilityImplementation myCapability = new MyCapabilityImplementation();
 *
 *         @Override
 *         public <T> T getCapability(@Nonnull Capability<T> capability, @Nonnull ForgeDirection side) {
 *             if (capability == MyCapability.CAPABILITY) {
 *                 return MyCapability.CAPABILITY.cast(myCapability);
 *             }
 *             return null;
 *         }
 *     }
 * }</pre>
 */
// spotless:on
public interface ICapabilityProvider {

    /**
     * Queries this provider for a capability implementation.
     * <p>
     * This method should:
     * <ul>
     * <li>Compare the requested capability with known capabilities using {@code ==} or do HashMap dispatch for a larger quantity of supported capability types.</li>
     * <li>If matched, use {@link Capability#cast} to return the implementation.</li>
     * <li>Return null if the capability is not supported.</li>
     * </ul>
     *
     * @param capability The capability instance being requested.
     * @param side       The {@link ForgeDirection} from which the capability is requested. Can be {@link ForgeDirection#UNKNOWN UNKNOWN} if the direction
     *                   is not relevant.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    @Nullable
    <T> T getCapability(@NotNull Capability<T> capability, @NotNull ForgeDirection side);
}
