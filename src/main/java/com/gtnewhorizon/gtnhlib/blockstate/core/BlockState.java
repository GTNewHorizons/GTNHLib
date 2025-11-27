package com.gtnewhorizon.gtnhlib.blockstate.core;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.geometry.TransformLike;

/// Represents the state of a block. Includes things like rotation, powered-ness, orientation, etc. Does not include
/// things like the inventory.
public interface BlockState extends AutoCloseable, Cloneable {

    BlockState clone();

    /// Gets the 'original' block for this state. Note that any block-changing properties (such as the various 'lit'
    /// properties) may change the actual block that gets placed by this BlockState.
    Block getBlock();

    <T> T getPropertyValue(BlockProperty<T> property);

    <T> T getPropertyValue(String name);

    <T> void setPropertyValue(BlockProperty<T> property, T value);

    <T> void setPropertyValue(String name, T value);

    /// Copies the properties stored in this BlockState into a map. Key=Property Name, Value=Property Value (as text).
    Map<String, String> toMap();

    void transform(TransformLike transform);

    void place(World world, int x, int y, int z);

    ItemStack getItemStack();

    @Override
    void close();
}
