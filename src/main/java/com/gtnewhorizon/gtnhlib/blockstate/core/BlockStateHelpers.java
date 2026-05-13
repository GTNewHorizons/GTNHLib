package com.gtnewhorizon.gtnhlib.blockstate.core;

import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

public class BlockStateHelpers {

    @Nullable
    public static <T> T get(IBlockAccess world, int x, int y, int z, BlockProperty<T> property) {
        if (world == null) return null;

        try (BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z)) {
            return state.getPropertyValue(property);
        }
    }

    @Nullable
    public static <T> T get(IBlockAccess world, int x, int y, int z, String name) {
        if (world == null) return null;

        try (BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z)) {
            return state.getPropertyValue(name);
        }
    }

    public static boolean has(IBlockAccess world, int x, int y, int z, BlockProperty<?> property) {
        if (world == null) return false;

        try (BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z)) {
            return state.getPropertyValue(property) != null;
        }
    }

    public static boolean has(IBlockAccess world, int x, int y, int z, String name) {
        if (world == null) return false;

        try (BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z)) {
            return state.getPropertyValue(name) != null;
        }
    }

    public static <T> void set(IBlockAccess world, int x, int y, int z, BlockProperty<T> property, T value) {
        if (world == null) return;

        try (BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z)) {
            state.setPropertyValue(property, value);
        }
    }

    public static <T> void set(IBlockAccess world, int x, int y, int z, String name, T value) {
        if (world == null) return;

        try (BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z)) {
            state.setPropertyValue(name, value);
        }
    }
}
