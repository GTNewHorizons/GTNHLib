package com.gtnewhorizon.gtnhlib.capability;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// spotless:off
/**
 * Utility class for retrieving capabilities from various Minecraft objects. This class provides convenience methods
 * that handle both capability-based and direct interface implementations, ensuring backward compatibility with
 * traditional interface implementations.
 * <p>
 * Example usage:
 * <pre>{@code
 *     // Retrieve capability from a TileEntity
 *     MyInterface impl = Capabilities.getCapability(tileEntity, MyCapability.class, ForgeDirection.NORTH);
 *
 *     // Retrieve capability from an ItemStack
 *     MyInterface impl = Capabilities.getCapability(itemStack, MyCapability.class);
 *
 *     // Retrieve capability from an Entity
 *     MyInterface impl = Capabilities.getCapability(entity, MyCapability.class);
 * }</pre>
 *
 * @see CapabilityProvider
 */
// spotless:on
@SuppressWarnings("unused")
public final class Capabilities {

    /**
     * Retrieves a capability from the given TileEntity.
     *
     * @param tileEntity The TileEntity to query.
     * @param capability The capability being requested.
     * @param side       The side of the TileEntity being queried.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    public static <T> T getCapability(@Nullable TileEntity tileEntity, @NotNull Class<T> capability,
            @NotNull ForgeDirection side) {
        return getCapability((Object) tileEntity, capability, side);
    }

    /**
     * Retrieves a capability from the given TileEntity without specifying a side.
     *
     * @param tileEntity The TileEntity to query.
     * @param capability The capability being requested.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    public static <T> T getCapability(@Nullable TileEntity tileEntity, @NotNull Class<T> capability) {
        return getCapability((Object) tileEntity, capability, ForgeDirection.UNKNOWN);
    }

    /**
     * Retrieves a capability from the given ItemStack's Item.
     *
     * @param itemStack  The ItemStack to query.
     * @param capability The capability being requested.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    public static <T> T getCapability(@Nullable ItemStack itemStack, @NotNull Class<T> capability) {
        if (itemStack == null) {
            return null;
        }
        return getCapability(itemStack.getItem(), capability, ForgeDirection.UNKNOWN);
    }

    /**
     * Retrieves a capability from the given Entity.
     *
     * @param entity     The Entity to query.
     * @param capability The capability being requested.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    public static <T> T getCapability(@Nullable Entity entity, @NotNull Class<T> capability) {
        return getCapability(entity, capability, ForgeDirection.UNKNOWN);
    }

    /**
     * Internal utility method that tries a direct cast if the object matches the capability's interface, then falls
     * back to querying {@link CapabilityProvider} if available.
     *
     * @param object     The object to query.
     * @param capability The capability being requested.
     * @param side       The side from which the capability is being requested.
     * @param <T>        The type of the capability interface.
     * @return The capability implementation, or null if not available.
     */
    private static <T> T getCapability(@Nullable Object object, @NotNull Class<T> capability,
            @NotNull ForgeDirection side) {
        if (object == null) {
            return null;
        }
        if (capability.isAssignableFrom(object.getClass())) {
            return capability.cast(object);
        }
        if (object instanceof CapabilityProvider provider) {
            return provider.getCapability(capability, side);
        }
        return null;
    }
}
