package com.gtnewhorizon.gtnhlib.blockstate.core;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/// A property whose value is solely determined by block metadata.
public interface MetaBlockProperty<T> extends BlockProperty<T> {

    int getMeta(T value, int existing);

    T getValue(int meta);

    @Override
    default boolean appliesTo(IBlockAccess world, int x, int y, int z, Block block, int meta, @Nullable TileEntity tile) {
        return appliesTo(meta);
    }

    @Override
    default boolean appliesTo(ItemStack stack, Item item, int meta) {
        return appliesTo(item.getMetadata(meta));
    }

    default boolean appliesTo(int meta) {
        return true;
    }

    /// Whether this property requires the existing meta value. When false, the parameter may be set to any value and
    /// must be ignored by the implementation.
    default boolean needsExisting() {
        return true;
    }

    @Override
    default T getValue(IBlockAccess world, int x, int y, int z) {
        if (!hasTrait(BlockPropertyTrait.SupportsWorld)) throw new UnsupportedOperationException();

        return getValue(world.getBlockMetadata(x, y, z));
    }

    @Override
    default void setValue(World world, int x, int y, int z, T value) {
        if (!hasTrait(BlockPropertyTrait.SupportsWorld)) throw new UnsupportedOperationException();

        boolean needsExisting = needsExisting();

        int existing = needsExisting ? world.getBlockMetadata(x, y, z) : 0;

        int meta = getMeta(value, existing);

        if (!needsExisting || existing != meta) {
            world.setBlockMetadataWithNotify(x, y, z, meta, 2);
        }
    }

    @Override
    default T getValue(ItemStack stack) {
        if (!hasTrait(BlockPropertyTrait.SupportsStacks)) throw new UnsupportedOperationException();

        @SuppressWarnings("DataFlowIssue")
        int meta = stack.getItem().getMetadata(stack.getItemDamage());

        return getValue(meta);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    default void setValue(ItemStack stack, T t) {
        if (!hasTrait(BlockPropertyTrait.SupportsStacks)) throw new UnsupportedOperationException();

        int meta = needsExisting() ? stack.getItem().getMetadata(stack.getItemDamage()) : 0;

        meta = getMeta(t, meta);

        stack.setItemDamage(((ItemBlock) stack.getItem()).field_150939_a.damageDropped(meta));
    }
}
