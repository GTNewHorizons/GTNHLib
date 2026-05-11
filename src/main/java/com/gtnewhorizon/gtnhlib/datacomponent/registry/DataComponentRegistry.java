package com.gtnewhorizon.gtnhlib.datacomponent.registry;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentMap;
import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentMapImpl;
import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentType;
import com.gtnewhorizon.gtnhlib.util.data.ImmutableItemMeta;
import com.gtnewhorizon.gtnhlib.util.data.ItemMeta;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class DataComponentRegistry {

    private DataComponentRegistry() {}

    private static class PropertyMap<K> extends Object2ObjectOpenHashMap<K, Map<String, DataComponentType<?>>> {

        public void add(K key, DataComponentType<?> prop) {
            this.computeIfAbsent(key, x -> new Object2ObjectArrayMap<>()).put(prop.getName(), prop);
        }

        public void copyAll(K key, Map<String, DataComponentType<?>> out) {
            var map = this.get(key);

            if (map != null) {
                out.putAll(map);
            }
        }
    }

    private static class CachedPropertyMap<K> extends PropertyMap<K> {

        private final Function<K, Map<String, DataComponentType<?>>> fn;

        public CachedPropertyMap(Function<K, Map<String, DataComponentType<?>>> fn) {
            this.fn = fn;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, DataComponentType<?>> get(Object k) {
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

    private static final PropertyMap<Item> ITEM_PROPERTIES = new PropertyMap<>();
    private static final PropertyMap<ImmutableItemMeta> ITEM_META_PROPERTIES = new PropertyMap<>();

    private static final PropertyMap<Type> IFACE_PROPERTIES = new PropertyMap<>();
    private static final CachedPropertyMap<Type> IFACE_CACHE = new CachedPropertyMap<>(
            DataComponentRegistry::getInterfaceProperties);

    private static final ObjectArrayList<DataComponentFactory<?>> CUSTOM_PROPERTIES = new ObjectArrayList<>();

    public static void registerComponent(Item item, DataComponentType<?> property) {
        ITEM_PROPERTIES.add(item, property);
    }

    public static void registerComponent(Collection<Item> items, DataComponentType<?> property) {
        for (Item item : items) {
            ITEM_PROPERTIES.add(item, property);
        }
    }

    public static void registerComponent(Item item, int itemMeta, DataComponentType<?> property) {
        registerComponent(new ItemMeta(item, itemMeta), property);
    }

    public static void registerComponent(ImmutableItemMeta im, DataComponentType<?> property) {
        ITEM_META_PROPERTIES.add(new ItemMeta(im), property);
    }

    public static void registerComponent(Type iface, DataComponentType<?> property) {
        IFACE_PROPERTIES.add(iface, property);
    }

    public static void registerComponent(DataComponentFactory<?> factory) {
        CUSTOM_PROPERTIES.add(factory);
    }

    @SuppressWarnings("unchecked")
    public static void registerComponent(DataComponentType<?> component) {
        registerComponent(new DataComponentFactory<>() {

            @Override
            public @NotNull DataComponentType<Object> getComponent(ItemStack stack, Item item, int meta) {
                return (DataComponentType<Object>) component;
            }
        });
    }

    @NotNull
    private static Map<String, DataComponentType<?>> getInterfaceProperties(Type clazz) {
        Map<String, DataComponentType<?>> cache = new Object2ObjectArrayMap<>();

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

    private static final ItemMeta POOLED_IM = new ItemMeta(Items.feather);

    public static void getComponents(ItemStack stack, Map<String, DataComponentType<?>> properties) {
        properties.clear();

        Item item = Objects.requireNonNull(stack.getItem(), "Item cannot be null: " + stack);
        int meta = stack.getItemDamage();

        ITEM_PROPERTIES.copyAll(item, properties);
        ITEM_META_PROPERTIES.copyAll(POOLED_IM.setItem(item).setItemMeta(meta), properties);
        IFACE_CACHE.copyAll(item.getClass(), properties);

        CUSTOM_PROPERTIES.forEach(factory -> {
            DataComponentType<?> property = factory.getComponent(stack, item, meta);

            if (property != null) {
                properties.put(property.getName(), property);
            }
        });
        properties.values().removeIf(comp -> !comp.appliesTo(stack, item, meta));
    }

    public static DataComponentMap getComponentMap(ItemStack stack) {
        DataComponentMapImpl componentMap = DataComponentMapImpl.getInstance();
        componentMap.fromStack(stack);
        return componentMap;
    }
}
