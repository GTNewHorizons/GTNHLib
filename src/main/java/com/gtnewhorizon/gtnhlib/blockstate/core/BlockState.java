package com.gtnewhorizon.gtnhlib.blockstate.core;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.geometry.TransformLike;

import cpw.mods.fml.common.registry.GameRegistry;

/// Represents the state of a block. Includes things like rotation, powered-ness, orientation, etc. Does not include
/// things like the inventory.
/// This is used as a generic block introspection and modification interface. Supported blocks will register their
/// properties in [BlockPropertyRegistry] and consumers of the API will be able to retrieve an opaque [BlockState]
/// object for a given block in the world, or a given [ItemStack].
///
/// This class differs from modern vanilla in several major ways:
/// 1. [BlockState] objects are not immutable, and they are not fixed references. You cannot use `==`
/// to check for [BlockState] equality, like you would in vanilla, as there is no precomputation of every state
/// permutation for a block.
/// 2. States are evaluated at runtime instead of game initialization. This means that it may not be possible to know
/// which properties are present on a given block until that block is in the world. While you can set these properties
/// via [#setPropertyValue(String, Object)], their validation/parsing will be deferred and their updates may be ignored
/// with a warning if they cannot be performed. A property in this state is called "deferred", due to the fact that it
/// is stored as a `string -> string/object` pair, instead of a `property -> object` pair like normal properties.
/// A [BlockState] can be pre-validated by calling [#reify(World, int, int, int)], but this still requires a valid block
/// in the world and may depend on state that is not captured by a [BlockState] such as tile data, leading to erroneous
/// property value modification/removal.
/// 3. [BlockState]s have opt-in pooling. Use
/// [BlockPropertyRegistry#getBlockState(BlockStatePool, IBlockAccess, int, int, int)] and
/// [BlockPropertyRegistry#getBlockState(BlockStatePool, ItemStack)] to pool states, which significantly reduce
/// allocations. [#close()] must be called to return a [BlockState] to the pool. [BlockStatePool]s cannot be overfilled,
/// so there's no need to worry about memory leaks. [BlockState#close()] can be elided for non-pooled [BlockState]s.
/// 4. Extra properties can be injected into blocks, but they may not be supported. Consumers can call
/// [#setPropertyValue(BlockProperty, Object)] with arbitrary properties, and those properties will be validated once
/// the state is placed or reified. A property in this state is called "unvalidated".
/// @apiNote While [BlockState]s will do their best to prevent crashes or undefined behaviour, passing invalid values to
/// any of the methods is considered illegal usage and should be avoided. Much of the state within a [BlockState] cannot
/// be easily validated, so there are plenty of log messages to capture runtime errors, but do not rely on them for
/// safety/stability.
public interface BlockState extends AutoCloseable, Cloneable {

    /// Creates an exact copy of this state, within the same pool (if pooled).
    BlockState clone();

    /// Clears this [BlockState] and sets its block to the given block reference. The [BlockState] will not have any
    /// properties after this method returns.
    void reset(Block block);

    /// Gets the 'original' block for this state. Note that any block-changing properties (such as the various 'lit'
    /// properties) may change the actual block that gets placed by this BlockState.
    Block getBlock();

    /// Gets the block metadata for this state by passing the given value through each property with the
    /// [BlockPropertyTrait#OnlyNeedsMeta] trait. Ignores non-meta properties entirely.
    /// Ignores deferred values. Treats unvalidated and validated properties the same.
    /// @param existing The existing meta, if needed. Can likely be set to 0.
    int getBlockMeta(int existing);

    /// Returns the resolution state of the given property in this [BlockState], or null if the property is not present.
    /// Deferred properties are not found by this overload — use [#getPropertyState(String)] to include them.
    @Nullable
    BlockPropertyState getPropertyState(BlockProperty<?> property);

    /// Returns the resolution state of the named property in this [BlockState], or null if no property with that
    /// name is present. Includes deferred properties.
    @Nullable
    BlockPropertyState getPropertyState(String name);

    /// Returns true if this state contains the given property (by reference). Deferred properties are not included.
    default boolean hasProperty(BlockProperty<?> property) {
        return getPropertyState(property) != null;
    }

    /// Returns true if this state contains a property with the given name. Includes deferred properties.
    default boolean hasProperty(String name) {
        return getPropertyState(name) != null;
    }

    /// Removes the property from this state (by reference). Does nothing if not present.
    /// Deferred properties are not removed by this overload — use [#removeProperty(String)] to remove them.
    void removeProperty(BlockProperty<?> property);

    /// Removes the property with the given name from this state. Does nothing if not present.
    /// Removes deferred properties as well.
    void removeProperty(String name);

    /// Gets the value of a property. Does not include deferred properties. Includes unvalidated properties.
    <T> T getPropertyValue(BlockProperty<T> property);

    /// Gets the value of a property by name. Does not include deferred properties. Includes unvalidated properties.
    /// May cause a [ClassCastException] if you aren't careful.
    default <T> T getPropertyValue(String name) {
        // noinspection unchecked
        return (T) getPropertyValue(name, false);
    }

    /// Gets the value of a property by name. Optionally includes deferred properties. Always includes unvalidated
    /// properties. The [Class] for the value of a deferred property is undefined - it is whatever was previously passed
    /// to [#setPropertyValue(String, Object)].
    Object getPropertyValue(String name, boolean includeDeferred);

    /// Sets a property value. If the property does not already exist in this state, the value is marked as
    /// "unvalidated" and will be validated when placed or during reification.
    <T> void setPropertyValue(BlockProperty<T> property, T value);

    /// Sets a property value. If the property does not already exist in this state, the value is marked as
    /// "deferred" and will be parsed and validated when placed or during reification.
    /// If there is a property with the same name, the given value will be validated against its type. If the given
    /// value does not match the property's type, and the value is a [String], it will be parsed via
    /// [BlockProperty#parse(String)]. Other types are undefined behaviour, but they will likely crash or log a message.
    <T> void setPropertyValue(String name, @Nullable T value);

    /// Copies all properties stored in this BlockState into a map. Key=Property Name, Value=Property Value (as text).
    Map<String, String> toMap();

    /// Transforms properties based on a given transform. Ignores deferred properties. Treats unvalidated properties the
    /// same as normal properties.
    void transform(TransformLike transform);

    /// Validates the property values stored in this state against a real block in the world.
    /// Removes invalid values, and converts deferred properties to normal properties.
    void reify(World world, int x, int y, int z);

    /// Validates the property values stored in this state against the given list of properties.
    /// Any values in this state that do not correspond to a property in the given map are removed.
    /// Deferred properties in this state are converted to unvalidated properties by calling
    /// [BlockProperty#parse(String)] on the matching property in the map.
    /// This does not validate properties, as validation requires a block in the world.
    void reify(Map<String, BlockProperty<?>> validProperties);

    /// Puts the base block for this state into the world and applies each property independently.
    ///
    /// @return true when successful
    boolean place(World world, int x, int y, int z, int flags);

    /// Gets a stack for this state, with any stack-supporting properties applied to it.
    ItemStack getItemStack();

    /// Returns this state to the pool, if it came from one.
    @Override
    void close();

    /// Visits every property value in this state, including [BlockPropertyState#DEFERRED] and
    /// [BlockPropertyState#UNVALIDATED] values.
    /// The [BlockPropertyValueConsumer#accept] method receives a null [BlockProperty] for deferred entries.
    /// Mutating this state during iteration produces undefined behaviour.
    void forEachValue(BlockPropertyValueConsumer consumer);

    /// Resets this state and copies the property values in `source` into this state.
    BlockState copy(BlockState source);

    /// Parses a [BlockState] from its canonical string form.
    ///
    /// Format: `modid:blockname[key=value,key2="escaped, value"]`
    ///
    /// - The block id and brackets are required; missing or mismatched brackets throw [IllegalArgumentException].
    /// - Values containing commas or equals signs must be quoted with `"`. A backslash inside a quoted value
    /// escapes the next character (`\\` → `\`, `\"` → `"`).
    /// - All property values are stored as deferred string entries; call [#reify] to resolve them.
    /// - If `pool` is non-null, the returned state is taken from the pool and must be [#close]d.
    ///
    /// @throws IllegalArgumentException when brackets are missing or mismatched
    static BlockState fromString(@Nullable BlockStatePool pool, String str) {
        int firstBrace = str.indexOf('[');
        int lastBrace = str.lastIndexOf(']');

        if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) {
            throw new IllegalArgumentException("Malformed BlockState string (missing or mismatched brackets): " + str);
        }

        String id = str.substring(0, firstBrace);

        String[] idHalves = id.split(":");

        Block block = GameRegistry.findBlock(
                idHalves.length == 1 ? "minecraft" : idHalves[0],
                idHalves.length == 1 ? idHalves[0] : idHalves[1]);

        BlockStateImpl state = pool != null ? pool.getInstance() : new BlockStateImpl();
        state.setBlock(block);

        String values = str.substring(firstBrace + 1, lastBrace);

        while (!values.isEmpty()) {
            int nextEq = values.indexOf('=');

            String valueName = values.substring(0, nextEq);
            String value;

            if (values.charAt(nextEq + 1) == '"') {
                char[] chars = values.substring(nextEq + 2).toCharArray();

                int i = 0;

                StringBuilder sb = new StringBuilder();

                for (; i < chars.length; i++) {
                    if (chars[i] == '\\') {
                        if (i < chars.length - 1) {
                            sb.append(chars[i + 1]);
                        }

                        i++;
                    } else if (chars[i] == '"') {
                        break;
                    } else {
                        sb.append(chars[i]);
                    }
                }

                value = sb.toString();
                // i points at the closing '"'; skip past it
                values = values.substring(nextEq + 2 + i + 1);
                // skip the comma separator that follows (non-quoted path handles this via nextComma)
                if (!values.isEmpty() && values.charAt(0) == ',') {
                    values = values.substring(1);
                }
            } else {
                int nextComma = values.indexOf(',');

                if (nextComma != -1) {
                    value = values.substring(nextEq + 1, nextComma);
                    values = values.substring(nextComma + 1);
                } else {
                    value = values.substring(nextEq + 1);
                    values = "";
                }
            }

            state.setPropertyValue(valueName, value);
        }

        return state;
    }
}
