package com.gtnewhorizon.gtnhlib.api.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;

/**
 * Registry for mods Like LittleBlocks or MetaWorlds that add virtual subworlds that need to be rendered into another
 * parent world. A subworld is addressed by a {@link WorldAddress}: a host dimension, a namespace, and a sub ID. The
 * host dimension is always an ordinary Forge dimension. The sub ID names a virtual world inside it, and the namespace
 * says which mod owns that sub ID — necessary because each mod keeps its own sub ID space. Every mod can register a
 * handler for subworlds it support, the registry allows lookup between (subworld) <-> (parent world, address). This can
 * be used to send information about a subworld between client and server.
 */
public final class WorldContextRegistry {

    /** Sub ID of the real world of a dimension, as opposed to a virtual world inside it. */
    public static final int ROOT_SUB_ID = 0;

    /** Returned by {@link Handler#getSubId} for a world the handler does not own. */
    public static final int UNKNOWN_SUB_ID = -1;

    /** Namespace of the real world of a dimension, which no handler owns. */
    public static final String ROOT_NAMESPACE = "";

    /** The full address of a world. */
    public static final class WorldAddress {

        /** Id of the parent Forge dimension */
        public final int hostDimensionId;

        /** Identifies the mod owning {@link #subId}, or {@link #ROOT_NAMESPACE} for an ordinary world. */
        public final String namespace;

        /** Which virtual world inside the host dimension, or {@link #ROOT_SUB_ID} for the host itself. */
        public final int subId;

        public WorldAddress(int hostDimensionId, String namespace, int subId) {
            if (namespace == null) {
                throw new IllegalArgumentException("World address namespace must not be null");
            }
            if (ROOT_NAMESPACE.equals(namespace) != (subId == ROOT_SUB_ID)) {
                throw new IllegalArgumentException(
                        "World address namespace '" + namespace
                                + "' and sub world id "
                                + subId
                                + " disagree on whether this is a root world");
            }
            this.hostDimensionId = hostDimensionId;
            this.namespace = namespace;
            this.subId = subId;
        }

        /** True for an ordinary world, whose namespace a sender may omit. */
        public boolean isRoot() {
            return ROOT_NAMESPACE.equals(namespace) && subId == ROOT_SUB_ID;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof WorldAddress)) return false;
            WorldAddress that = (WorldAddress) other;
            return this.hostDimensionId == that.hostDimensionId && this.namespace.equals(that.namespace)
                    && this.subId == that.subId;
        }

        @Override
        public int hashCode() {
            int hash = Fnv1a32.initialState();
            hash = Fnv1a32.hashStep(hash, this.hostDimensionId);
            hash = Fnv1a32.hashStep(hash, this.namespace);
            hash = Fnv1a32.hashStep(hash, this.subId);
            return hash;
        }

        @Override
        public String toString() {
            return "WorldAddress[dimension=" + this.hostDimensionId
                    + ", namespace="
                    + this.namespace
                    + ", sub="
                    + this.subId
                    + "]";
        }
    }

    public interface Handler {

        /**
         * Gets a subworld from a host world.
         *
         * @return {@code null} for host worlds or sub IDs this handler does not own.
         */
        default World getSubWorld(World hostWorld, int subId) {
            return null;
        }

        /**
         * Gets the id of a subworld. This is also how the registry decides which handler owns a world, so a handler
         * claiming one here must also answer {@link #getHostWorld} for it. Owned sub IDs must be positive, since
         * {@link WorldContextRegistry#ROOT_SUB_ID} names the host world itself.
         *
         * @return {@link #UNKNOWN_SUB_ID} for worlds this handler does not own.
         */
        default int getSubId(World world) {
            return UNKNOWN_SUB_ID;
        }

        /**
         * Gets the real world for a given subworld.
         *
         * @return {@code null} for worlds this handler does not own.
         */
        default World getHostWorld(World world) {
            return null;
        }

        /**
         * Lists the subworlds this handler currently holds inside a host world. Must not include the host itself.
         *
         * @return an empty collection for host worlds this handler has no worlds in.
         */
        default Collection<World> getSubWorlds(World hostWorld) {
            return Collections.emptyList();
        }
    }

    private static final Map<String, Handler> handlers = new LinkedHashMap<>();

    private WorldContextRegistry() {}

    /**
     * Registers a handler under a namespace unique to the owning mod, such as {@code "littleblocks"}. The namespace is
     * chosen by the mod and must be stable across versions and between client and server, because the on-wire key is
     * derived from it.
     *
     * @throws IllegalArgumentException if the namespace is empty or already registered.
     */
    public static void registerHandler(String namespace, Handler handler) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("World context handler namespace must not be empty");
        }
        if (handlers.containsKey(namespace)) {
            throw new IllegalArgumentException("Namespace '" + namespace + "' is already registered");
        }
        handlers.put(namespace, handler);
    }

    /**
     * Gets the address for a subworld. Returns a root address for an ordinary world.
     *
     * @throws RuntimeException if a handler claims the world through {@link Handler#getSubId} but does not name its
     *                          host world.
     */
    public static WorldAddress addressOf(World world) {
        for (Map.Entry<String, Handler> entry : handlers.entrySet()) {
            Handler handler = entry.getValue();
            int subId = handler.getSubId(world);
            if (subId == UNKNOWN_SUB_ID) continue;
            if (subId == ROOT_SUB_ID) {
                throw new RuntimeException(
                        "Handler '" + entry.getKey()
                                + "' claims a world with the root sub world id "
                                + ROOT_SUB_ID
                                + "; sub world ids must be positive");
            }
            World hostWorld = handler.getHostWorld(world);
            if (hostWorld == null) {
                throw new RuntimeException(
                        "Handler '" + entry.getKey() + "' claims sub world id " + subId + " but names no host world");
            }
            return new WorldAddress(hostWorld.provider.dimensionId, entry.getKey(), subId);
        }
        return new WorldAddress(world.provider.dimensionId, ROOT_NAMESPACE, ROOT_SUB_ID);
    }

    /**
     * Gets the host world for a subworld, returning the argument itself for an ordinary world.
     */
    public static World getHostWorld(World world) {
        for (Handler handler : handlers.values()) {
            World hostWorld = handler.getHostWorld(world);
            if (hostWorld != null) return hostWorld;
        }
        return world;
    }

    /**
     * Lists every world sharing a host with the given one, the host itself first. For an ordinary world in a game with
     * no virtual worlds this is a singleton, so callers can iterate unconditionally.
     */
    public static List<World> getWorlds(World world) {
        World hostWorld = getHostWorld(world);
        List<World> worlds = new ArrayList<>();
        worlds.add(hostWorld);
        for (Handler handler : handlers.values()) {
            worlds.addAll(handler.getSubWorlds(hostWorld));
        }
        return worlds;
    }

    /**
     * Gets a subworld with the given address for a host world, returning {@code hostWorld} itself for a root address.
     *
     * @throws RuntimeException if no handler is registered under the address' namespace, or it does not own that sub ID
     *                          within that host.
     */
    public static World getSubWorld(World hostWorld, WorldAddress address) {
        if (address.isRoot()) return hostWorld;
        Handler handler = handlers.get(address.namespace);
        if (handler == null) {
            throw new RuntimeException(
                    "No world context handler registered under namespace '" + address.namespace + "'");
        }
        World world = handler.getSubWorld(hostWorld, address.subId);
        if (world != null) return world;
        throw new RuntimeException(
                "Handler '" + address.namespace
                        + "' does not own sub world id "
                        + address.subId
                        + " in dimension "
                        + hostWorld.provider.dimensionId);
    }

    /**
     * Gets a client world by its address. The client is only ever in one dimension, so the host dimension must be the
     * one it currently occupies.
     *
     * @throws RuntimeException if the client is not in that dimension, or the sub world cannot be resolved within it.
     */
    public static World getClientWorld(WorldAddress address) {
        World hostWorld = ClientWorldAccess.getCurrentWorld();
        if (hostWorld == null || hostWorld.provider.dimensionId != address.hostDimensionId) {
            throw new RuntimeException("Missing mapping for dimension id " + address.hostDimensionId);
        }
        return getSubWorld(hostWorld, address);
    }

    /**
     * Gets a server world by its address.
     *
     * @throws RuntimeException if the dimension is not loaded, or the sub world cannot be resolved within it.
     */
    public static World getServerWorld(WorldAddress address) {
        World hostWorld = DimensionManager.getWorld(address.hostDimensionId);
        if (hostWorld == null) {
            throw new RuntimeException("Missing mapping for dimension id " + address.hostDimensionId);
        }
        return getSubWorld(hostWorld, address);
    }

    /** Keep client-only code out of the registry's common-side class loading path. */
    private static final class ClientWorldAccess {

        private static World getCurrentWorld() {
            return net.minecraft.client.Minecraft.getMinecraft().theWorld;
        }
    }
}
