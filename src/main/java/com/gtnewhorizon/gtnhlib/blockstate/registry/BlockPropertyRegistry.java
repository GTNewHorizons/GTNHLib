package com.gtnewhorizon.gtnhlib.blockstate.registry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStateImpl;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockStatePool;
import com.gtnewhorizon.gtnhlib.util.data.BlockMeta;
import com.gtnewhorizon.gtnhlib.util.data.ImmutableBlockMeta;
import com.gtnewhorizon.gtnhlib.util.data.ImmutableItemMeta;
import com.gtnewhorizon.gtnhlib.util.data.ItemMeta;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class BlockPropertyRegistry {

    @FunctionalInterface
    public interface PropertyConsumer extends Consumer<BlockProperty<?>> {

    }

    private BlockPropertyRegistry() {}

    private static class PropertyMap<K> extends Object2ObjectOpenHashMap<K, Map<String, BlockProperty<?>>> {

        public void add(K key, BlockProperty<?> prop) {
            this.computeIfAbsent(key, x -> new Object2ObjectArrayMap<>()).put(prop.getName(), prop);
        }

        public void readAll(K key, PropertyConsumer consumer) {
            var map = this.get(key);

            if (map != null) {
                map.values().forEach(consumer);
            }
        }
    }

    private static class CachedPropertyMap<K> extends PropertyMap<K> {

        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        private final Function<K, Map<String, BlockProperty<?>>> fn;

        public CachedPropertyMap(Function<K, Map<String, BlockProperty<?>>> fn) {
            this.fn = fn;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, BlockProperty<?>> get(Object k) {

            Map<String, BlockProperty<?>> map;

            try {
                lock.readLock().lock();
                map = super.get(k);
            } finally {
                lock.readLock().unlock();
            }

            // There's a timing gap here where another thread could call `get` with the same key, but since this is just
            // a cache we don't care about that - we'll just occasionally do double the work.

            if (map == null) {
                map = fn.apply((K) k);

                try {
                    lock.writeLock().lock();

                    this.put(
                            (K) k,
                            map == null || map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(map));
                } finally {
                    lock.writeLock().unlock();
                }
            }

            return map;
        }
    }

    private static final PropertyMap<Block> BLOCK_PROPERTIES = new PropertyMap<>();
    private static final PropertyMap<ImmutableBlockMeta> BLOCK_META_PROPERTIES = new PropertyMap<>();

    private static final PropertyMap<Item> ITEM_PROPERTIES = new PropertyMap<>();
    private static final PropertyMap<ImmutableItemMeta> ITEM_META_PROPERTIES = new PropertyMap<>();

    private static final PropertyMap<Type> IFACE_PROPERTIES = new PropertyMap<>();
    private static final CachedPropertyMap<Type> IFACE_CACHE = new CachedPropertyMap<>(
            BlockPropertyRegistry::getInterfaceProperties);

    private static final ArrayList<BlockPropertyFactory<?>> CUSTOM_PROPERTIES = new ArrayList<>();

    /// Registers the property on the block (see [#registerProperty(Block, BlockProperty)]) and additionally creates
    /// a read-only item property for the block's item form (obtained via [Item#getItemFromBlock(Block)]) that always
    /// returns `defaultValue`. Use this overload when the in-world property has no meaningful stack representation and
    /// you only need a stable default for UI/display purposes.
    public static <T> void registerBlockItemProperty(Block block, BlockProperty<T> property, T defaultValue) {
        registerProperty(block, property);
        registerProperty(Item.getItemFromBlock(block), new BlockProperty<T>() {

            @Override
            public String getName() {
                return property.getName();
            }

            @Override
            public Type getType() {
                return property.getType();
            }

            @Override
            public T getValue(ItemStack stack) {
                return defaultValue;
            }

            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return trait == BlockPropertyTrait.SupportsStacks;
            }

            @Override
            public String stringify(T t) {
                return property.stringify(t);
            }
        });
    }

    /// Registers the property on both the block given and its block item.
    ///
    /// @throws IllegalArgumentException if the property doesn't support itemstacks.
    public static <T> void registerBlockItemProperty(Block block, BlockProperty<T> property) {
        if (!property.hasTrait(BlockPropertyTrait.SupportsStacks))
            throw new IllegalArgumentException("BlockItem property should support ItemStacks!");

        registerProperty(block, property);
        registerProperty(Item.getItemFromBlock(block), property);
    }

    /// Registers a property on a block, regardless of metadata. The property is returned for all queries against
    /// that block. Use this when the property applies to every metadata variant of the block.
    public static void registerProperty(Block block, BlockProperty<?> property) {
        BLOCK_PROPERTIES.add(block, property);
    }

    /// Convenience overload of [#registerProperty(Block, BlockProperty)] for registering the same property on
    /// multiple blocks at once.
    public static void registerProperty(Collection<Block> blocks, BlockProperty<?> property) {
        for (Block block : blocks) {
            BLOCK_PROPERTIES.add(block, property);
        }
    }

    /// Registers a property on a specific block+metadata combination. The property is only returned when the
    /// queried block has exactly that metadata value. Use this when the property only applies to one variant.
    public static void registerProperty(Block block, int blockMeta, BlockProperty<?> property) {
        registerProperty(new BlockMeta(block, blockMeta), property);
    }

    /// Registers a property on a specific [ImmutableBlockMeta] key (block + metadata combination).
    public static void registerProperty(ImmutableBlockMeta bm, BlockProperty<?> property) {
        BLOCK_META_PROPERTIES.add(new BlockMeta(bm), property);
    }

    /// Registers a property on an item, regardless of item damage/metadata. The item must be an [ItemBlock].
    /// The property is returned for all stack queries against that item.
    public static void registerProperty(Item item, BlockProperty<?> property) {
        if (!(item instanceof ItemBlock)) throw new IllegalArgumentException("Item must be an ItemBlock: " + item);

        ITEM_PROPERTIES.add(item, property);
    }

    /// Registers a property on a specific item+metadata combination. The item must be an [ItemBlock].
    public static void registerProperty(Item item, int itemMeta, BlockProperty<?> property) {
        registerProperty(new ItemMeta(item, itemMeta), property);
    }

    /// Registers a property on a specific [ImmutableItemMeta] key (item + metadata combination).
    /// The item must be an [ItemBlock].
    public static void registerProperty(ImmutableItemMeta im, BlockProperty<?> property) {
        if (!(im.getItem() instanceof ItemBlock))
            throw new IllegalArgumentException("Item must be an ItemBlock: " + im);

        ITEM_META_PROPERTIES.add(new ItemMeta(im), property);
    }

    /// Registers a property on an interface or class type. The property is returned for any block or tile entity
    /// whose class hierarchy includes `iface`. Use this to attach properties to all blocks implementing a shared
    /// interface (e.g. a "rotatable block" interface) without enumerating each block individually.
    public static void registerProperty(Type iface, BlockProperty<?> property) {
        IFACE_PROPERTIES.add(iface, property);
    }

    /// Registers a [BlockPropertyFactory] whose [BlockPropertyFactory#getProperty] is called on every query.
    /// Use this when the applicable property depends on dynamic world/stack state that cannot be expressed as
    /// a static block/item/interface registration.
    public static void registerProperty(BlockPropertyFactory<?> factory) {
        CUSTOM_PROPERTIES.add(factory);
    }

    @NotNull
    private static Map<String, BlockProperty<?>> getInterfaceProperties(Type clazz) {
        Map<String, BlockProperty<?>> cache = new Object2ObjectArrayMap<>();

        ObjectLinkedOpenHashSet<Type> queue = new ObjectLinkedOpenHashSet<>();

        queue.add(clazz);

        while (!queue.isEmpty()) {
            Type curr = queue.removeFirst();

            IFACE_PROPERTIES.readAll(curr, prop -> cache.put(prop.getName(), prop));

            if (curr instanceof Class<?>clazz2) {
                for (Type iface : clazz2.getGenericInterfaces()) {
                    queue.addAndMoveToFirst(iface);
                }

                for (Type iface : clazz2.getInterfaces()) {
                    queue.addAndMoveToFirst(iface);
                }

                if (clazz2.getSuperclass() != null && clazz2.getSuperclass() != Object.class) {
                    queue.add(clazz2.getGenericSuperclass());
                    queue.add(clazz2.getSuperclass());
                }
            }
        }

        return cache;
    }

    private static final BlockMeta POOLED_BM = new BlockMeta(Blocks.air);

    public static void getPossibleProperties(Block block, int meta, PropertyConsumer consumer) {
        BLOCK_PROPERTIES.readAll(block, consumer);

        synchronized (POOLED_BM) {
            BLOCK_META_PROPERTIES.readAll(POOLED_BM.setBlock(block).setBlockMeta(meta), consumer);
        }

        IFACE_CACHE.readAll(block.getClass(), consumer);
    }

    public static void getPossibleProperties(IBlockAccess world, int x, int y, int z, PropertyConsumer consumer) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        TileEntity tile;

        if (block.hasTileEntity(meta)) {
            tile = world.getTileEntity(x, y, z);

            if (tile != null) {
                IFACE_CACHE.readAll(tile.getClass(), consumer);
            }
        } else {
            tile = null;
        }

        getPossibleProperties(block, meta, consumer);

        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < CUSTOM_PROPERTIES.size(); i++) {
            BlockPropertyFactory<?> factory = CUSTOM_PROPERTIES.get(i);
            BlockProperty<?> property = factory.getProperty(world, x, y, z, block, meta, tile);

            if (property != null) {
                consumer.accept(property);
            }
        }
    }

    public static void getValidProperties(IBlockAccess world, int x, int y, int z, PropertyConsumer consumer) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        TileEntity tile;

        if (block.hasTileEntity(meta)) {
            tile = world.getTileEntity(x, y, z);
        } else {
            tile = null;
        }

        PropertyConsumer validatingConsumer = prop -> {
            if (!prop.appliesTo(world, x, y, z, block, meta, tile)) return;

            consumer.accept(prop);
        };

        if (tile != null) {
            IFACE_CACHE.readAll(tile.getClass(), validatingConsumer);
        }

        getPossibleProperties(block, meta, validatingConsumer);

        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < CUSTOM_PROPERTIES.size(); i++) {
            BlockPropertyFactory<?> factory = CUSTOM_PROPERTIES.get(i);
            BlockProperty<?> property = factory.getProperty(world, x, y, z, block, meta, tile);

            if (property != null) {
                validatingConsumer.accept(property);
            }
        }
    }

    private static final ItemMeta POOLED_IM = new ItemMeta(Items.feather);

    public static void getPossibleProperties(Item item, int meta, PropertyConsumer consumer) {
        ITEM_PROPERTIES.readAll(item, consumer);

        synchronized (POOLED_IM) {
            ITEM_META_PROPERTIES.readAll(POOLED_IM.setItem(item).setItemMeta(meta), consumer);
        }

        IFACE_CACHE.readAll(item.getClass(), consumer);
    }

    public static void getPossibleProperties(ItemStack stack, BlockPropertyRegistry.PropertyConsumer consumer) {
        Item item = Objects.requireNonNull(stack.getItem(), "Item cannot be null: " + stack);
        int meta = Items.feather.getDamage(stack);

        getPossibleProperties(item, meta, consumer);

        // noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < CUSTOM_PROPERTIES.size(); i++) {
            BlockPropertyFactory<?> factory = CUSTOM_PROPERTIES.get(i);

            BlockProperty<?> property = factory.getProperty(stack, item, meta);

            if (property != null) {
                consumer.accept(property);
            }
        }
    }

    public static void getValidProperties(ItemStack stack, BlockPropertyRegistry.PropertyConsumer consumer) {
        Item item = Objects.requireNonNull(stack.getItem(), "Item cannot be null: " + stack);
        int meta = Items.feather.getDamage(stack);

        PropertyConsumer validatingConsumer = prop -> {
            if (!prop.appliesTo(stack, item, meta)) return;

            consumer.accept(prop);
        };

        getPossibleProperties(item, meta, validatingConsumer);

        for (int i = 0; i < CUSTOM_PROPERTIES.size(); i++) {
            BlockPropertyFactory<?> factory = CUSTOM_PROPERTIES.get(i);

            BlockProperty<?> property = factory.getProperty(stack, item, meta);

            if (property != null) {
                validatingConsumer.accept(property);
            }
        }
    }

    /// Builds a [BlockState] from a block and metadata value without requiring a world position.
    /// Only properties with the [BlockPropertyTrait#OnlyNeedsMeta] trait (i.e. [MetaBlockProperty] implementations)
    /// are included; properties that require world or tile entity access are omitted.
    /// All entries are marked [BlockPropertyState#NORMAL].
    /// The returned state is not pooled and does not need to be [BlockState#close]d.
    @SuppressWarnings("resource")
    public static BlockState getBlockState(Block block, int meta) {
        return new BlockStateImpl().fromBlockMeta(block, meta);
    }

    /// Pooled overload of [#getBlockState(Block, int)]. The returned state must be [BlockState#close]d.
    public static BlockState getBlockState(BlockStatePool pool, Block block, int meta) {
        return pool.getInstance().fromBlockMeta(block, meta);
    }

    @SuppressWarnings("resource")
    public static BlockState getBlockState(IBlockAccess world, int x, int y, int z) {
        return new BlockStateImpl().fromWorld(world, x, y, z);
    }

    public static BlockState getBlockState(BlockStatePool pool, IBlockAccess world, int x, int y, int z) {
        return pool.getInstance().fromWorld(world, x, y, z);
    }

    @SuppressWarnings("resource")
    public static BlockState getBlockState(ItemStack stack) {
        return new BlockStateImpl().fromStack(stack);
    }

    public static BlockState getBlockState(BlockStatePool pool, ItemStack stack) {
        return pool.getInstance().fromStack(stack);
    }
}
