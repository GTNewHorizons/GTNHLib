package com.gtnewhorizon.gtnhlib.blockstate.registry;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
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
import com.gtnewhorizon.gtnhlib.util.data.BlockMeta;
import com.gtnewhorizon.gtnhlib.util.data.ImmutableBlockMeta;
import com.gtnewhorizon.gtnhlib.util.data.ImmutableItemMeta;
import com.gtnewhorizon.gtnhlib.util.data.ItemMeta;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class BlockPropertyRegistry {

    private BlockPropertyRegistry() {}

    private static class PropertyMap<K> extends Object2ObjectOpenHashMap<K, Map<String, BlockProperty<?>>> {

        public void add(K key, BlockProperty<?> prop) {
            this.computeIfAbsent(key, x -> new Object2ObjectArrayMap<>()).put(prop.getName(), prop);
        }

        public void copyAll(K key, Map<String, BlockProperty<?>> out) {
            var map = this.get(key);

            if (map != null) {
                out.putAll(map);
            }
        }
    }

    private static class CachedPropertyMap<K> extends PropertyMap<K> {

        private final Function<K, Map<String, BlockProperty<?>>> fn;

        public CachedPropertyMap(Function<K, Map<String, BlockProperty<?>>> fn) {
            this.fn = fn;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, BlockProperty<?>> get(Object k) {
            var map = super.get(k);

            if (map == null) {
                map = fn.apply((K) k);

                this.put(
                        (K) k,
                        map == null || map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(map));
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

    private static final ObjectArrayList<BlockPropertyFactory<?>> CUSTOM_PROPERTIES = new ObjectArrayList<>();

    public static void registerProperty(Block block, BlockProperty<?> property) {
        BLOCK_PROPERTIES.add(block, property);
    }

    public static void registerProperty(Collection<Block> blocks, BlockProperty<?> property) {
        for (Block block : blocks) {
            BLOCK_PROPERTIES.add(block, property);
        }
    }

    public static void registerProperty(Block block, int blockMeta, BlockProperty<?> property) {
        registerProperty(new BlockMeta(block, blockMeta), property);
    }

    public static void registerProperty(ImmutableBlockMeta bm, BlockProperty<?> property) {
        BLOCK_META_PROPERTIES.add(new BlockMeta(bm), property);
    }

    public static void registerProperty(Item item, BlockProperty<?> property) {
        if (!(item instanceof ItemBlock)) throw new IllegalArgumentException("Item must be an ItemBlock: " + item);

        ITEM_PROPERTIES.add(item, property);
    }

    public static void registerProperty(Item item, int itemMeta, BlockProperty<?> property) {
        registerProperty(new ItemMeta(item, itemMeta), property);
    }

    public static void registerProperty(ImmutableItemMeta im, BlockProperty<?> property) {
        if (!(im.getItem() instanceof ItemBlock))
            throw new IllegalArgumentException("Item must be an ItemBlock: " + im);

        ITEM_META_PROPERTIES.add(new ItemMeta(im), property);
    }

    public static void registerProperty(Type iface, BlockProperty<?> property) {
        IFACE_PROPERTIES.add(iface, property);
    }

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

            IFACE_PROPERTIES.copyAll(curr, cache);

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

    public static void getProperties(IBlockAccess world, int x, int y, int z,
            Map<String, BlockProperty<?>> properties) {
        properties.clear();

        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        BLOCK_PROPERTIES.copyAll(block, properties);
        BLOCK_META_PROPERTIES.copyAll(POOLED_BM.setBlock(block).setBlockMeta(meta), properties);
        IFACE_CACHE.copyAll(block.getClass(), properties);

        TileEntity tile;

        if (block.hasTileEntity(meta)) {
            tile = world.getTileEntity(x, y, z);

            if (tile != null) {
                IFACE_CACHE.copyAll(tile.getClass(), properties);
            }
        } else {
            tile = null;
        }

        CUSTOM_PROPERTIES.forEach(factory -> {
            BlockProperty<?> property = factory.getProperty(world, x, y, z, block, meta, tile);

            if (property != null) {
                properties.put(property.getName(), property);
            }
        });

        properties.entrySet().removeIf(e -> {
            if (!e.getValue().appliesTo(world, x, y, z, block, meta, tile)) return true;
            return !e.getValue().hasTrait(BlockPropertyTrait.SupportsWorld);
        });
    }

    private static final ItemMeta POOLED_IM = new ItemMeta(Items.feather);

    public static void getProperties(ItemStack stack, Map<String, BlockProperty<?>> properties) {
        properties.clear();

        Item item = Objects.requireNonNull(stack.getItem(), "Item cannot be null: " + stack);
        int meta = Items.feather.getDamage(stack);

        ITEM_PROPERTIES.copyAll(item, properties);
        ITEM_META_PROPERTIES.copyAll(POOLED_IM.setItem(item).setItemMeta(meta), properties);
        IFACE_CACHE.copyAll(item.getClass(), properties);

        CUSTOM_PROPERTIES.forEach(factory -> {
            BlockProperty<?> property = factory.getProperty(stack, item, meta);

            if (property != null) {
                properties.put(property.getName(), property);
            }
        });

        properties.entrySet().removeIf(e -> {
            if (!e.getValue().appliesTo(stack, item, meta)) return true;
            return !e.getValue().hasTrait(BlockPropertyTrait.SupportsStacks);
        });
    }

    public static BlockState getBlockState(IBlockAccess world, int x, int y, int z) {
        BlockStateImpl state = BlockStateImpl.getInstance();

        state.fromWorld(world, x, y, z);

        return state;
    }

    public static BlockState getBlockState(ItemStack stack) {
        BlockStateImpl state = BlockStateImpl.getInstance();

        state.fromStack(stack);

        return state;
    }
}
