package com.gtnewhorizon.gtnhlib.datacomponent.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.ApiStatus;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.datacomponent.registry.DataComponentRegistry;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

@SuppressWarnings({ "resource", "unchecked" })
@ApiStatus.Internal
public class DataComponentMapImpl implements DataComponentMap {

    private Item item;

    private final Map<String, DataComponentType<?>> components = new HashMap<>(4);
    private final Map<DataComponentType<?>, Object> values = new HashMap<>(4);

    private static final ObjectPooler<DataComponentMapImpl> POOL = new ObjectPooler<>(DataComponentMapImpl::new);

    public static DataComponentMapImpl getInstance() {
        return POOL.getInstance().assertIsDefault();
    }

    public DataComponentMapImpl assertIsDefault() {
        if (item != null) throw new RuntimeException(
                "DataComponentMapImpl reference was mutated while in the pool; item was set to " + item);
        if (!components.isEmpty()) throw new RuntimeException(
                "DataComponentMapImpl reference was mutated while in the pool; components was set to " + components);
        if (!values.isEmpty()) throw new RuntimeException(
                "DataComponentMapImpl reference was mutated while in the pool; values was set to " + values);

        return this;
    }

    public DataComponentMapImpl reset() {
        this.item = null;
        this.components.clear();
        this.values.clear();
        return this;
    }

    public DataComponentMapImpl copy(DataComponentMapImpl other) {
        reset();

        this.item = other.item;
        this.components.putAll(other.components);
        this.values.putAll(other.values);

        return this;
    }

    public DataComponentMapImpl fromStack(ItemStack stack) {
        this.item = (Item) Objects.requireNonNull(stack.getItem(), "Item cannot be null");

        DataComponentRegistry.getComponents(stack, this.components);

        this.values.clear();

        this.components.forEach((name, component) -> { this.values.put(component, component.getValue(stack)); });

        return this;
    }

    @Override
    public DataComponentMap clone() {
        return POOL.getInstance().copy(this);
    }

    @Override
    public Item getItem() {
        return item;
    }

    @Override
    public <T> T get(DataComponentType<T> type) {
        return (T) values.get(type);
    }

    @Override
    public <T> T get(String name) {
        DataComponentType<T> component = (DataComponentType<T>) components.get(name);

        if (component == null) return null;

        return (T) values.get(component);
    }

    @Override
    public <T> void set(DataComponentType<T> type, T value) {
        if (components.containsValue(type)) {
            values.put(type, value);
        }
    }

    @Override
    public <T> void set(String name, T value) {
        DataComponentType<T> property = (DataComponentType<T>) components.get(name);

        if (property == null) {
            GTNHLib.LOG.warn(
                    "Tried to set invalid property on ComponentMap by name. Name={}, Value={}",
                    name,
                    value,
                    new Exception());
            return;
        }

        if (property.getType() instanceof Class<?>clazz) {
            if (!clazz.isInstance(value)) {
                GTNHLib.LOG.warn(
                        "Tried to set value for property on ComponentMap to an incompatible value. Name={}, Value={}",
                        name,
                        value,
                        new Exception());
                return;
            }
        }

        values.put(property, value);
    }

    @Override
    public <T> boolean has(DataComponentType<T> type) {
        return values.containsKey(type);
    }

    @Override
    public <T> boolean has(String name) {
        return components.containsKey(name);
    }

    @Override
    public Map<String, String> toMap() {
        Map<String, String> out = new Object2ObjectOpenHashMap<>(values.size());

        components.forEach((name, prop) -> {
            Object value = values.get(prop);

            if (value != null) {
                out.put(name, ((DataComponentType<Object>) prop).stringify(value));
            }
        });

        return out;
    }

    @Override
    public void close() {
        POOL.releaseInstance(this.reset());
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + values.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DataComponentMapImpl other)) return false;
        return this.item == other.item && this.values.equals(other.values);
    }
}
