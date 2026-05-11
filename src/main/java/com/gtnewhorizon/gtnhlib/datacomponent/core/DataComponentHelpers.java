package com.gtnewhorizon.gtnhlib.datacomponent.core;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.datacomponent.registry.DataComponentRegistry;

public class DataComponentHelpers {

    @Nullable
    public static <T> T get(ItemStack stack, DataComponentType<T> componentType) {
        if (stack == null || stack.getItem() == null) return null;

        try (DataComponentMap component = DataComponentRegistry.getComponentMap(stack)) {
            return component.get(componentType);
        }
    }

    @Nullable
    public static <T> T get(ItemStack stack, String name) {
        if (stack == null || stack.getItem() == null) return null;

        try (DataComponentMap component = DataComponentRegistry.getComponentMap(stack)) {
            return component.get(name);
        }
    }

    public static boolean has(ItemStack stack, DataComponentType<?> componentType) {
        if (stack == null || stack.getItem() == null) return false;

        try (DataComponentMap componentMap = DataComponentRegistry.getComponentMap(stack)) {
            return componentMap.has(componentType);
        }
    }

    public static boolean has(ItemStack stack, String name) {
        if (stack == null || stack.getItem() == null) return false;

        try (DataComponentMap componentMap = DataComponentRegistry.getComponentMap(stack)) {
            return componentMap.has(name);
        }
    }

    public static <T> void set(ItemStack stack, DataComponentType<T> componentType, T value) {
        if (stack == null || stack.getItem() == null) return;

        try (DataComponentMap componentMap = DataComponentRegistry.getComponentMap(stack)) {
            componentMap.set(componentType, value);
        }
    }

    public static <T> void set(ItemStack stack, String name, T value) {
        if (stack == null || stack.getItem() == null) return;

        try (DataComponentMap componentMap = DataComponentRegistry.getComponentMap(stack)) {
            componentMap.set(name, value);
        }
    }
}
