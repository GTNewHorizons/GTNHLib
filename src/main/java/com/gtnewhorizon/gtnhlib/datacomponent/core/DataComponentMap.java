package com.gtnewhorizon.gtnhlib.datacomponent.core;

import java.util.Map;

import net.minecraft.item.Item;

/// Represents the component of a itemStack. Includes things like item damaged, max stack, energy, etc. Does not include
/// things like the inventory.
public interface DataComponentMap extends AutoCloseable, Cloneable {

    DataComponentMap clone();

    /// Gets the 'original' item for this component. Note that any item-changing components (such as the various 'item
    /// damaged'
    /// components) may change the actual item that gets placed by this component.
    Item getItem();

    <T> T get(DataComponentType<T> type);

    <T> T get(String name);

    <T> void set(DataComponentType<T> type, T value);

    <T> void set(String name, T value);

    <T> boolean has(DataComponentType<T> type);

    <T> boolean has(String name);

    /// Copies the properties stored in this ComponentMap into a map. Key=Property Name, Value=Component Value (as
    /// text).
    Map<String, String> toMap();

    @Override
    void close();
}
