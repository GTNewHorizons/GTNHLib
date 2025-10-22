package com.gtnewhorizon.gtnhlib.blockstate.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.ApiStatus;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;
import com.gtnewhorizon.gtnhlib.geometry.TransformLike;
import com.gtnewhorizon.gtnhlib.geometry.VectorTransform;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@SuppressWarnings({ "resource", "unchecked" })
@ApiStatus.Internal
public class BlockStateImpl implements BlockState {

    private Block block;
    private int meta;

    private final Map<String, BlockProperty<?>> properties = new HashMap<>(4);
    private final Map<BlockProperty<?>, Object> values = new HashMap<>(4);

    private static final ObjectPooler<BlockStateImpl> POOL = new ObjectPooler<>(BlockStateImpl::new);

    public static BlockStateImpl getInstance() {
        return POOL.getInstance().assertIsDefault();
    }

    public BlockStateImpl assertIsDefault() {
        if (block != null) throw new RuntimeException(
                "BlockStateImpl reference was mutated while in the pool; block was set to " + block);
        if (meta != 0) throw new RuntimeException(
                "BlockStateImpl reference was mutated while in the pool; meta was set to " + meta);
        if (!properties.isEmpty()) throw new RuntimeException(
                "BlockStateImpl reference was mutated while in the pool; properties was set to " + properties);
        if (!values.isEmpty()) throw new RuntimeException(
                "BlockStateImpl reference was mutated while in the pool; values was set to " + values);

        return this;
    }

    public BlockStateImpl reset() {
        this.block = null;
        this.meta = 0;
        this.properties.clear();
        this.values.clear();

        return this;
    }

    public BlockStateImpl copy(BlockStateImpl other) {
        reset();

        this.block = other.block;
        this.meta = other.meta;
        this.properties.putAll(other.properties);
        this.values.putAll(other.values);

        return this;
    }

    public BlockStateImpl fromWorld(IBlockAccess world, int x, int y, int z) {
        this.block = world.getBlock(x, y, z);
        this.meta = world.getBlockMetadata(x, y, z);

        BlockPropertyRegistry.getProperties(world, x, y, z, this.properties);

        this.values.clear();

        this.properties.forEach((name, property) -> { this.values.put(property, property.getValue(world, x, y, z)); });

        return this;
    }

    public BlockStateImpl fromStack(ItemStack stack) {
        ItemBlock itemBlock = (ItemBlock) Objects.requireNonNull(stack.getItem(), "Item cannot be null");

        this.block = itemBlock.field_150939_a;
        this.meta = itemBlock.getMetadata(stack.getItemDamage());

        BlockPropertyRegistry.getProperties(stack, this.properties);

        this.values.clear();

        this.properties.forEach((name, property) -> { this.values.put(property, property.getValue(stack)); });

        return this;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public BlockStateImpl clone() {
        return POOL.getInstance().copy(this);
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public <T> T getPropertyValue(BlockProperty<T> property) {
        return (T) values.get(property);
    }

    @Override
    public <T> T getPropertyValue(String name) {
        BlockProperty<T> property = (BlockProperty<T>) properties.get(name);

        if (property == null) return null;

        return (T) values.get(property);
    }

    @Override
    public <T> void setPropertyValue(BlockProperty<T> property, T value) {
        if (properties.containsValue(property)) {
            values.put(property, value);
        }
    }

    @Override
    public <T> void setPropertyValue(String name, T value) {
        BlockProperty<T> property = (BlockProperty<T>) properties.get(name);

        if (property == null) {
            GTNHLib.LOG.warn(
                    "Tried to set invalid property on BlockState by name. Name={}, Value={}",
                    name,
                    value,
                    new Exception());
            return;
        }

        if (property.getType() instanceof Class<?>clazz) {
            if (!clazz.isInstance(value)) {
                GTNHLib.LOG.warn(
                        "Tried to set value for property on BlockState to an incompatible value. Name={}, Value={}",
                        name,
                        value,
                        new Exception());
                return;
            }
        }

        values.put(property, value);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> out = new Object2ObjectOpenHashMap<>(values.size());

        properties.forEach((name, prop) -> {
            Object value = values.get(prop);

            if (value != null) {
                out.put(name, ((BlockProperty<Object>) prop).stringify(value));
            }
        });

        return out;
    }

    @Override
    public void transform(TransformLike transform) {
        values.replaceAll((property, value) -> {
            if (property.hasTrait(BlockPropertyTrait.VectorTransformable)
                    && transform instanceof VectorTransform vector) {
                return ((VectorTransformableProperty<Object>) property).transform(value, vector);
            }

            if (property.hasTrait(BlockPropertyTrait.Transformable) && transform instanceof DirectionTransform dir) {
                return ((TransformableProperty<Object>) property).transform(value, dir);
            }

            return value;
        });
    }

    @Override
    public void place(World world, int x, int y, int z) {
        world.setBlock(x, y, z, this.block, this.meta, 2);

        values.forEach((property, value) -> {
            if (property.hasTrait(BlockPropertyTrait.SupportsWorld)
                    && property.hasTrait(BlockPropertyTrait.WorldMutable)) {
                ((BlockProperty<Object>) property).setValue(world, x, y, z, value);
            }
        });
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(this.block, 1, this.block.damageDropped(this.meta));

        values.forEach((property, value) -> {
            if (property.hasTrait(BlockPropertyTrait.SupportsStacks)
                    && property.hasTrait(BlockPropertyTrait.StackMutable)) {
                ((BlockProperty<Object>) property).setValue(stack, value);
            }
        });

        return stack;
    }

    @Override
    public void close() {
        POOL.releaseInstance(this.reset());
    }
}
