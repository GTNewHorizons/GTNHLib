package com.gtnewhorizon.gtnhlib.blockstate.core;

import static com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait.StackMutable;
import static com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait.SupportsStacks;
import static com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait.SupportsWorld;
import static com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait.WorldMutable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;
import com.gtnewhorizon.gtnhlib.geometry.TransformLike;
import com.gtnewhorizon.gtnhlib.geometry.VectorTransform;
import com.gtnewhorizon.gtnhlib.hash.Fnv1a32;
import com.gtnewhorizon.gtnhlib.util.IObjectPool;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;

/// See [BlockState] header comment for the API contract and high-level behaviour of this type.
@SuppressWarnings({ "resource", "unchecked", "ForLoopReplaceableByForEach", "rawtypes" })
@ApiStatus.Internal
public class BlockStateImpl implements BlockState {

    private final IObjectPool<BlockStateImpl> pool;

    @Getter
    @Setter
    private Block block;

    private final List<@NotNull PropertyEntry> entries = new ArrayList<>(8);

    /// A property value is stored as a pooled entry in what is essentially a multi-keyed map. This is used to simplify
    /// state
    /// tracking, since a property value can transition between deferred, unvalidated, or normal states arbitrarily.
    private static class PropertyEntry {

        /// When the name is null, this entry is empty and doesn't contain any information
        @Nullable
        public String name;
        /// When the property is null, this entry is deferred.
        @Nullable
        public BlockProperty property;
        /// This should never be null when this entry is populated, but some properties may support null values. The
        /// nullability for this field is largely based on vibes, hopes, and wishes.
        @Nullable
        public Object value;
        /// When true, [#property] was loaded from a live block or item and [BlockProperty#appliesTo] can be skipped.
        /// Does NOT mean the value is correct for the block at any specific world position.
        public boolean validated;

        public void reset() {
            name = null;
            property = null;
            value = null;
            validated = false;
        }

        public void copy(PropertyEntry other) {
            if (other.name == null) return;

            this.name = other.name;
            this.property = other.property;

            if (this.property != null) {
                this.value = this.property.copy(other.value);
            } else {
                // No way to copy this, let's hope it's immutable/not pooled
                this.value = other.value;
            }

            this.validated = other.validated;
        }

        public boolean isDeferred() {
            return property == null;
        }

        public void normal(BlockProperty property, @Nullable Object value) {
            this.name = property.getName();
            this.property = property;
            this.value = value;
            this.validated = true;
        }

        public void unvalidated(BlockProperty property, @Nullable Object value) {
            this.name = property.getName();
            this.property = property;
            this.value = value;
            this.validated = false;
        }

        public void deferred(String name, @Nullable Object value) {
            this.name = name;
            this.property = null;
            this.value = value;
            this.validated = false;
        }

        @Override
        public String toString() {
            return "PropertyEntry{" + "name='"
                    + name
                    + '\''
                    + ", property="
                    + property
                    + ", value="
                    + value
                    + ", validated="
                    + validated
                    + '}';
        }
    }

    public BlockStateImpl() {
        this(null);
    }

    BlockStateImpl(IObjectPool<BlockStateImpl> pool) {
        this.pool = pool;
    }

    public BlockStateImpl assertIsDefault() {
        if (block != null) {
            throw new RuntimeException(
                    "BlockStateImpl reference was mutated while in the pool; block was set to " + block);
        }

        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name != null) {
                throw new RuntimeException(
                        "BlockStateImpl reference was mutated while in the pool; values was set to " + entries);
            }
        }

        return this;
    }

    @Override
    public void reset(Block block) {
        this.block = block;

        // There will rarely be more than 8 properties on a block, we can trim the extra (in case there are any, to
        // prevent memory leaks).
        while (entries.size() > 8) {
            entries.remove(entries.size() - 1);
        }

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).reset();
        }
    }

    public BlockStateImpl reset() {
        reset(null);

        return this;
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.releaseInstance(this);
        }
    }

    @Override
    public int getBlockMeta(int existing) {
        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;
            if (entry.property == null) continue;
            if (!entry.property.hasTrait(BlockPropertyTrait.OnlyNeedsMeta)) continue;

            existing = ((MetaBlockProperty) entry.property).getMeta(entry.value, existing);
        }

        return existing;
    }

    @Override
    public void forEachValue(BlockPropertyValueConsumer consumer) {
        for (int i = 0; i < entries.size(); i++) {
            BlockStateImpl.PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            BlockPropertyState state;

            if (!entry.isDeferred()) {
                if (entry.validated) {
                    state = BlockPropertyState.NORMAL;
                } else {
                    state = BlockPropertyState.UNVALIDATED;
                }
            } else {
                state = BlockPropertyState.DEFERRED;
            }

            consumer.accept(entry.name, state, entry.property, entry.value);
        }
    }

    @Override
    public BlockStateImpl copy(BlockState source) {
        reset();

        this.block = source.getBlock();

        if (source instanceof BlockStateImpl impl) {
            // Faster allocation-less copy for impl -> impl

            List<@NotNull PropertyEntry> sourceEntries = impl.entries;

            while (entries.size() < sourceEntries.size()) {
                entries.add(new PropertyEntry());
            }

            for (int i = 0; i < sourceEntries.size(); i++) {
                PropertyEntry src = sourceEntries.get(i);
                PropertyEntry dst = this.entries.get(i);

                dst.copy(src);
            }
        } else {
            // Use an anonymous class so that we can use a mutable index field
            source.forEachValue(new BlockPropertyValueConsumer() {

                private int index;

                @Override
                public void accept(String name, BlockPropertyState state, @Nullable BlockProperty property,
                        Object value) {
                    if (index >= entries.size()) {
                        entries.add(new PropertyEntry());
                    }

                    PropertyEntry entry = entries.get(index++);

                    entry.name = name;
                    entry.property = property;
                    entry.value = value;
                    entry.validated = state == BlockPropertyState.NORMAL;
                }
            });
        }

        return this;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public BlockStateImpl clone() {
        BlockStateImpl cloned = pool != null ? pool.getInstance() : new BlockStateImpl();
        return cloned.copy(this);
    }

    private int findIndex(BlockProperty prop) {
        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.property == prop) {
                return i;
            }
        }

        return -1;
    }

    private int findIndex(String name) {
        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (Objects.equals(entry.name, name)) {
                return i;
            }
        }

        return -1;
    }

    private int findOrAddFreeIndex() {
        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) return i;
        }

        entries.add(new PropertyEntry());

        return entries.size() - 1;
    }

    /// Removes unused entries from this block state to reduce iteration costs
    private void compact() {
        this.entries.removeIf(e -> e.name == null);
    }

    /// Returns true if [valueClass] is the same type as [propertyType], accounting for primitive/boxed equivalence.
    private static boolean typeMatches(Type propertyType, Class<?> valueClass) {
        if (propertyType == valueClass) return true;
        if (propertyType == boolean.class) return valueClass == Boolean.class;
        if (propertyType == int.class) return valueClass == Integer.class;
        if (propertyType == float.class) return valueClass == Float.class;
        if (propertyType == double.class) return valueClass == Double.class;
        if (propertyType == long.class) return valueClass == Long.class;
        if (propertyType == byte.class) return valueClass == Byte.class;
        if (propertyType == short.class) return valueClass == Short.class;
        if (propertyType == char.class) return valueClass == Character.class;
        return false;
    }

    public BlockStateImpl fromWorld(IBlockAccess world, int x, int y, int z) {
        this.block = world.getBlock(x, y, z);

        BlockPropertyRegistry.getValidProperties(world, x, y, z, prop -> {
            int index = findIndex(prop.getName());

            if (index == -1) {
                index = findOrAddFreeIndex();
            }

            PropertyEntry entry = entries.get(index);

            if (entry.property != null && entry.property != prop) {
                // Block has two properties with the same name: logic error
                GTNHLib.LOG.warn(
                        "Block {} has two properties with the name {}: {} and {}",
                        block,
                        entry.name,
                        entry.property,
                        prop);
                return;
            }

            entry.normal(prop, prop.getValue(world, x, y, z));
        });

        return this;
    }

    public BlockStateImpl fromStack(ItemStack stack) {
        ItemBlock itemBlock = (ItemBlock) Objects.requireNonNull(stack.getItem(), "Item cannot be null");

        this.block = itemBlock.field_150939_a;

        BlockPropertyRegistry.getValidProperties(stack, prop -> {
            int index = findIndex(prop.getName());

            if (index == -1) {
                index = findOrAddFreeIndex();
            }

            PropertyEntry entry = entries.get(index);

            if (entry.property != null && entry.property != prop) {
                // Block has two properties with the same name: logic error
                GTNHLib.LOG.warn(
                        "Item {} (block {}) has two properties with the name {}: {} and {}",
                        itemBlock,
                        block,
                        entry.name,
                        entry.property,
                        prop);
                return;
            }

            entry.normal(prop, prop.getValue(stack));
        });

        return this;
    }

    private static BlockPropertyState entryState(PropertyEntry entry) {
        if (entry.isDeferred()) return BlockPropertyState.DEFERRED;
        return entry.validated ? BlockPropertyState.NORMAL : BlockPropertyState.UNVALIDATED;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public BlockStateImpl fromBlockMeta(Block block, int meta) {
        this.block = block;

        BlockPropertyRegistry.getPossibleProperties(block, meta, prop -> {
            if (!prop.hasTrait(BlockPropertyTrait.OnlyNeedsMeta)) return;

            int index = findIndex(prop.getName());

            if (index == -1) {
                index = findOrAddFreeIndex();
            }

            PropertyEntry entry = entries.get(index);

            if (entry.property != null && entry.property != prop) {
                GTNHLib.LOG.warn(
                        "Block {} has two properties with the name {}: {} and {}",
                        block,
                        entry.name,
                        entry.property,
                        prop);
                return;
            }

            entry.normal(prop, ((MetaBlockProperty) prop).getValue(meta));
        });

        return this;
    }

    @Override
    public BlockPropertyState getPropertyState(BlockProperty<?> property) {
        int index = findIndex(property);
        return index == -1 ? null : entryState(entries.get(index));
    }

    @Override
    public BlockPropertyState getPropertyState(String name) {
        int index = findIndex(name);
        return index == -1 ? null : entryState(entries.get(index));
    }

    @Override
    public void removeProperty(BlockProperty<?> property) {
        int index = findIndex(property);
        if (index != -1) entries.get(index).reset();
    }

    @Override
    public void removeProperty(String name) {
        int index = findIndex(name);
        if (index != -1) entries.get(index).reset();
    }

    @Override
    public <T> T getPropertyValue(BlockProperty<T> property) {
        int index = findIndex(property);

        return index == -1 ? null : (T) entries.get(index).value;
    }

    @Override
    public Object getPropertyValue(String name, boolean includeDeferred) {
        int index = findIndex(name);

        if (index == -1) return null;

        PropertyEntry entry = entries.get(index);

        if (entry.isDeferred() && !includeDeferred) return null;

        return entry.value;
    }

    @Override
    public <T> void setPropertyValue(BlockProperty<T> property, T value) {
        int index = findIndex(property);

        if (index == -1) {
            // Fall back to name lookup so that a previously-deferred entry for the same
            // property is upgraded in-place rather than creating a duplicate name entry.
            index = findIndex(property.getName());
        }

        if (index == -1) {
            index = findOrAddFreeIndex();
            entries.get(index).unvalidated(property, value);
        } else {
            entries.get(index).normal(property, value);
        }
    }

    @Override
    public <T> void setPropertyValue(String name, T value) {
        int index = findIndex(name);

        if (index == -1) {
            index = findOrAddFreeIndex();

            entries.get(index).deferred(name, value);
        } else {
            PropertyEntry entry = entries.get(index);

            if (entry.property != null) {
                // Not deferred: we have a property reference to validate against

                if (value == null) {
                    entry.value = null;
                } else if (typeMatches(entry.property.getType(), value.getClass())) {
                    entry.value = value;
                } else if (value instanceof String str) {
                    entry.value = entry.property.parse(str);
                } else {
                    GTNHLib.LOG.warn(
                            "Tried to set property on BlockState to invalid type. Name={}, Value={}, Type={}, Expected={}",
                            name,
                            value,
                            value.getClass(),
                            entry.property.getType(),
                            new IllegalBlockStateException());
                }
            } else {
                // Deferred: we have no property reference, and we just have to pray the user knows what they're doing

                entry.deferred(name, value);
            }
        }
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> out = new Object2ObjectOpenHashMap<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            if (entry.property != null) {
                out.put(entry.name, entry.property.stringify(entry.value));
            } else {
                out.put(entry.name, String.valueOf(entry.value));
            }
        }

        return out;
    }

    @Override
    public void transform(TransformLike transform) {
        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            if (entry.property != null) {
                if (entry.property.hasTrait(BlockPropertyTrait.VectorTransformable)
                        && transform instanceof VectorTransform vector) {
                    entry.value = ((VectorTransformableProperty<Object>) entry.property).transform(entry, vector);
                } else if (entry.property.hasTrait(BlockPropertyTrait.Transformable)
                        && transform instanceof DirectionTransform dir) {
                            entry.value = ((TransformableProperty<Object>) entry.property).transform(entry, dir);
                        }
            } else {
                GTNHLib.LOG.warn(
                        "Transforming BlockState with a deferred property: this will do nothing, which is likely not what you want. Block={}, Property={}, Value={}, Transform={}",
                        block,
                        entry.name,
                        entry.value,
                        transform,
                        new IllegalBlockStateException());
            }
        }
    }

    @Override
    public void reify(World world, int x, int y, int z) {
        Map<String, BlockProperty> availableProperties = null;

        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            if (entry.property != null) {
                if (!entry.validated) {
                    // Value was injected by code, we need to make sure we can actually set it on this block
                    if (!entry.property.appliesTo(world, x, y, z)) {
                        GTNHLib.LOG.warn(
                                "Tried to set block property on block that it does not apply to. Block={}, Property Name={}, Property={}, Value={}",
                                this.block,
                                entry.name,
                                entry.property,
                                entry.value,
                                new IllegalBlockStateException());
                        entry.reset();
                        continue;
                    }
                }

                if (!entry.property.hasTrait(SupportsWorld) || !entry.property.hasTrait(WorldMutable)) {
                    GTNHLib.LOG.warn(
                            "Tried to set immutable or non-world-supporting property on block. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            new IllegalBlockStateException());
                    entry.reset();
                }
            } else {
                // Deferred, we need to fetch the available properties and match the name to one

                if (availableProperties == null) {
                    availableProperties = new HashMap<>();
                    Map<String, BlockProperty> finalAvailableProperties = availableProperties;
                    BlockPropertyRegistry.getValidProperties(
                            world,
                            x,
                            y,
                            z,
                            prop -> finalAvailableProperties.put(prop.getName(), prop));
                }

                BlockProperty match = availableProperties.get(entry.name);

                if (match == null) {
                    GTNHLib.LOG.warn(
                            "Tried to set deferred block property on block that it does not exist on. Block={}, Property Name={}, Value={}, Available Properties={}",
                            this.block,
                            entry.name,
                            entry.value,
                            availableProperties,
                            new IllegalBlockStateException());
                    entry.reset();
                    continue;
                }

                if (!match.appliesTo(world, x, y, z)) {
                    entry.reset();
                    GTNHLib.LOG.warn(
                            "Tried to set deferred block property on block that it does not apply to. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            new IllegalBlockStateException());
                    continue;
                }

                if (!match.hasTrait(SupportsWorld) || !match.hasTrait(WorldMutable)) {
                    GTNHLib.LOG.warn(
                            "Tried to set immutable or non-world-supporting property on block. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            match,
                            entry.value,
                            new IllegalBlockStateException());
                    entry.reset();
                    continue;
                }

                if (entry.value == null) {
                    // I sure hope this is valid :shrug:
                    entry.normal(match, null);
                } else if (typeMatches(match.getType(), entry.value.getClass())) {
                    entry.normal(match, entry.value);
                } else if (entry.value instanceof String str) {
                    entry.normal(match, match.parse(str));
                } else {
                    GTNHLib.LOG.warn(
                            "Passed invalid value type to deferred block property. Block={}, Property Name={}, Property={}, Value={}, Actual Type={}, Expected Type={}",
                            this.block,
                            entry.name,
                            match,
                            entry.value,
                            entry.value.getClass(),
                            match.getType(),
                            new IllegalBlockStateException());
                    entry.reset();
                }
            }
        }

        compact();
    }

    @Override
    public void reify(Map<String, BlockProperty<?>> validProperties) {
        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            if (entry.property != null) {
                // Not deferred

                if (!validProperties.containsKey(entry.name)) {
                    GTNHLib.LOG.warn(
                            "BlockState contained missing property. Block={}, Property Name={}, Property={}, Value={}, Valid Properties={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            validProperties,
                            new IllegalBlockStateException());
                    entry.reset();
                    continue;
                }

                if (!entry.property.hasTrait(SupportsWorld)) {
                    GTNHLib.LOG.warn(
                            "Tried to set non-world-supporting property on block. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            new IllegalBlockStateException());
                    entry.reset();
                }
            } else {
                // Deferred, we need to fetch the available properties and match the name to one

                BlockProperty match = validProperties.get(entry.name);

                if (match == null) {
                    GTNHLib.LOG.warn(
                            "Tried to reify deferred block property on block that it does not exist on. Block={}, Property Name={}, Value={}, Valid Properties={}",
                            this.block,
                            entry.name,
                            entry.value,
                            validProperties,
                            new IllegalBlockStateException());
                    entry.reset();
                    continue;
                }

                if (!match.hasTrait(SupportsWorld)) {
                    GTNHLib.LOG.warn(
                            "Tried to reify non-world-supporting property on block. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            match,
                            entry.value,
                            new IllegalBlockStateException());
                    entry.reset();
                    continue;
                }

                if (entry.value == null) {
                    // I sure hope this is valid :shrug:
                    entry.unvalidated(match, null);
                } else if (typeMatches(match.getType(), entry.value.getClass())) {
                    entry.unvalidated(match, entry.value);
                } else if (entry.value instanceof String str) {
                    entry.unvalidated(match, match.parse(str));
                } else {
                    GTNHLib.LOG.warn(
                            "Passed invalid value type to deferred block property. Block={}, Property Name={}, Property={}, Value={}, Actual Type={}, Expected Type={}",
                            this.block,
                            entry.name,
                            match,
                            entry.value,
                            entry.value.getClass(),
                            match.getType(),
                            new IllegalBlockStateException());
                    entry.reset();
                }
            }
        }

        compact();
    }

    @Override
    public boolean place(World world, int x, int y, int z, int flags) {
        world.setBlock(x, y, z, this.block, 0, flags);

        Map<String, BlockProperty> availableProperties = null;

        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            if (entry.property != null) {
                if (!entry.validated) {
                    // Value was injected by code, we need to make sure we can actually set it on this block
                    if (!entry.property.appliesTo(world, x, y, z)) {
                        GTNHLib.LOG.warn(
                                "Tried to set block property on block that it does not apply to. Block={}, Property Name={}, Property={}, Value={}",
                                this.block,
                                entry.name,
                                entry.property,
                                entry.value,
                                new IllegalBlockStateException());
                        continue;
                    }
                }

                if (!entry.property.hasTrait(SupportsWorld)) {
                    GTNHLib.LOG.warn(
                            "Tried to set non-world-supporting property on block. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            new IllegalBlockStateException());
                    continue;
                }

                if (entry.property.hasTrait(WorldMutable)) {
                    // silently ignore immutable properties
                    entry.property.setValue(world, x, y, z, entry.value);
                }
            } else {
                // Deferred, we need to fetch the available properties and match the name to one

                if (availableProperties == null) {
                    availableProperties = new HashMap<>();
                    Map<String, BlockProperty> finalAvailableProperties = availableProperties;
                    BlockPropertyRegistry.getValidProperties(
                            world,
                            x,
                            y,
                            z,
                            prop -> finalAvailableProperties.put(prop.getName(), prop));
                }

                BlockProperty match = availableProperties.get(entry.name);

                if (match == null) {
                    GTNHLib.LOG.warn(
                            "Tried to set deferred block property on block that it does not exist on. Block={}, Property Name={}, Value={}, Available Properties={}",
                            this.block,
                            entry.name,
                            entry.value,
                            availableProperties,
                            new IllegalBlockStateException());
                    continue;
                }

                if (!match.appliesTo(world, x, y, z)) {
                    GTNHLib.LOG.warn(
                            "Tried to set deferred block property on block that it does not apply to. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            new IllegalBlockStateException());
                    continue;
                }

                Object value;

                if (entry.value == null) {
                    // I sure hope this is valid :shrug:
                    value = null;
                } else if (typeMatches(match.getType(), entry.value.getClass())) {
                    value = entry.value;
                } else if (entry.value instanceof String str) {
                    value = match.parse(str);
                } else {
                    GTNHLib.LOG.warn(
                            "Passed invalid value type to deferred block property. Block={}, Property Name={}, Property={}, Value={}, Actual Type={}, Expected Type={}",
                            this.block,
                            entry.name,
                            match,
                            entry.value,
                            entry.value.getClass(),
                            match.getType(),
                            new IllegalBlockStateException());
                    continue;
                }

                if (!match.hasTrait(SupportsWorld)) {
                    GTNHLib.LOG.warn(
                            "Tried to set non-world-supporting property on block. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            match,
                            entry.value,
                            new IllegalBlockStateException());
                    continue;
                }

                if (match.hasTrait(WorldMutable)) {
                    // silently ignore immutable properties
                    match.setValue(world, x, y, z, value);
                }
            }
        }

        return true;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(this.block, 1);

        Map<String, BlockProperty> availableProperties = null;

        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            if (entry.property != null) {
                if (!entry.validated) {
                    // Value was injected by code, we need to make sure we can actually set it on this stack
                    if (!entry.property.appliesTo(stack)) {
                        GTNHLib.LOG.warn(
                                "Tried to set block property on stack that it does not apply to. Block={}, Property Name={}, Property={}, Value={}",
                                this.block,
                                entry.name,
                                entry.property,
                                entry.value,
                                new IllegalBlockStateException());
                        continue;
                    }
                }

                if (!entry.property.hasTrait(SupportsStacks)) {
                    GTNHLib.LOG.warn(
                            "Tried to set immutable or non-stack-supporting property on stack. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            new IllegalBlockStateException());
                    continue;
                }

                if (entry.property.hasTrait(StackMutable)) {
                    // silently ignore immutable properties
                    entry.property.setValue(stack, entry.value);
                }
            } else {
                // Deferred, we need to fetch the available properties and match the name to one

                if (availableProperties == null) {
                    availableProperties = new HashMap<>();
                    Map<String, BlockProperty> finalAvailableProperties = availableProperties;
                    BlockPropertyRegistry
                            .getValidProperties(stack, prop -> finalAvailableProperties.put(prop.getName(), prop));
                }

                BlockProperty match = availableProperties.get(entry.name);

                if (match == null) {
                    GTNHLib.LOG.warn(
                            "Tried to set deferred block property on stack that it does not exist on. Block={}, Property Name={}, Value={}, Available Properties={}",
                            this.block,
                            entry.name,
                            entry.value,
                            availableProperties,
                            new IllegalBlockStateException());
                    continue;
                }

                if (!match.appliesTo(stack)) {
                    GTNHLib.LOG.warn(
                            "Tried to set deferred block property on stack that it does not apply to. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            entry.property,
                            entry.value,
                            new IllegalBlockStateException());
                    continue;
                }

                Object resolvedValue;

                if (entry.value == null) {
                    resolvedValue = null;
                } else if (typeMatches(match.getType(), entry.value.getClass())) {
                    resolvedValue = entry.value;
                } else if (entry.value instanceof String str) {
                    resolvedValue = match.parse(str);
                } else {
                    GTNHLib.LOG.warn(
                            "Passed invalid value type to deferred block property. Block={}, Property Name={}, Property={}, Value={}, Actual Type={}, Expected Type={}",
                            this.block,
                            entry.name,
                            match,
                            entry.value,
                            entry.value.getClass(),
                            match.getType(),
                            new IllegalBlockStateException());
                    continue;
                }

                if (!match.hasTrait(SupportsStacks)) {
                    GTNHLib.LOG.warn(
                            "Tried to set non-stack-supporting property on stack. Block={}, Property Name={}, Property={}, Value={}",
                            this.block,
                            entry.name,
                            match,
                            resolvedValue,
                            new IllegalBlockStateException());
                    continue;
                }

                if (match.hasTrait(StackMutable)) {
                    // silently ignore immutable properties
                    match.setValue(stack, resolvedValue);
                }
            }
        }

        return stack;
    }

    @Override
    public int hashCode() {
        int hash = Fnv1a32.initialState();
        if (block != null) hash = Fnv1a32.hashStep(hash, block.hashCode());

        // Accumulate entry contributions with addition (commutative) so the hash is order-independent,
        // matching the order-independent behaviour of equals(). Values are normalized to text so that
        // a deferred "true" and a non-deferred Boolean.TRUE produce the same hash contribution.
        int entriesHash = 0;
        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);
            if (entry.name == null) continue;
            String textValue = entry.property == null ? String.valueOf(entry.value)
                    : entry.property.stringify(entry.value);
            entriesHash += entry.name.hashCode() * 31 + Objects.hashCode(textValue);
        }

        hash = Fnv1a32.hashStep(hash, entriesHash);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockStateImpl other)) return false;

        if (this.block != other.block) return false;

        BitSet remainingOther = new BitSet();

        for (int i = 0; i < other.entries.size(); i++) {
            PropertyEntry entry = other.entries.get(i);

            if (entry.name == null) continue;

            remainingOther.set(i);
        }

        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            int otherIndex = -1;

            if (entry.property != null) {
                // Try to find by property first since that's much faster
                otherIndex = other.findIndex(entry.property);
            }

            if (otherIndex == -1) otherIndex = other.findIndex(entry.name);

            if (otherIndex == -1) return false;

            remainingOther.clear(otherIndex);

            PropertyEntry otherEntry = other.entries.get(otherIndex);

            if (entry.isDeferred() == otherEntry.isDeferred()) {
                if (!entry.isDeferred() && !otherEntry.isDeferred()) {
                    // Both have properties, make sure they're actually the same
                    if (entry.property != otherEntry.property) return false;
                }

                // If either both are deferred or both are not deferred, we can just do an object equality check
                if (!Objects.equals(entry.value, otherEntry.value)) return false;
            } else {
                // One or the other is deferred, both have to be converted to text

                String ourValue = entry.property == null ? String.valueOf(entry.value)
                        : entry.property.stringify(entry.value);
                String theirValue = otherEntry.property == null ? String.valueOf(otherEntry.value)
                        : otherEntry.property.stringify(otherEntry.value);

                if (!Objects.equals(ourValue, theirValue)) return false;
            }
        }

        return remainingOther.isEmpty();
    }

    @Override
    public String toString() {
        UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);

        List<String> values = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            PropertyEntry entry = entries.get(i);

            if (entry.name == null) continue;

            String value;

            if (entry.property != null) {
                value = entry.property.stringify(entry.value);
            } else {
                value = String.valueOf(entry.value);
            }

            if (value.contains(",") || value.contains("=")) {
                value = '"' + value.replace("\"", "\\\"") + '"';
            }

            values.add(entry.name + "=" + value);
        }

        values.sort(String::compareTo);

        return id + "[" + String.join(",", values) + "]";
    }
}
