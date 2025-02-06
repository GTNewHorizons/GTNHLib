package com.gtnewhorizon.gtnhlib.capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.common.util.ForgeDirection;

// spotless:off
/**
 * Classes implementing this interface indicates that it is able to offer certain {@link Capability}.
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
 *         public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable ForgeDirection side) {
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
     * <li>Compare the requested capability with known capabilities using {@code ==}.</li>
     * <li>If matched, use {@link Capability#cast} to return the implementation.</li>
     * <li>Return null if the capability is not supported.</li>
     * </ul>
     *
     * @param capability The capability instance being requested.
     * @param side       The {@link ForgeDirection} from which the capability is requested. Can be null if direction is
     *                   not relevant.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    @Nullable
    <T> T getCapability(@Nonnull Capability<T> capability, @Nullable ForgeDirection side);
}
