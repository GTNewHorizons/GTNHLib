package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;

public interface FloatBlockProperty extends BlockProperty<Float> {

    @Override
    default Type getType() {
        return float.class;
    }

    /// Primitive (non-boxing) world getter. Implementors may override for efficiency.
    default float getFloatValue(IBlockAccess world, int x, int y, int z) {
        return getValue(world, x, y, z);
    }

    /// Primitive (non-boxing) world setter. Implementors may override for efficiency.
    default void setFloatValue(World world, int x, int y, int z, float value) {
        setValue(world, x, y, z, value);
    }

    /// Primitive (non-boxing) stack getter. Implementors may override for efficiency.
    default float getFloatValue(ItemStack stack) {
        return getValue(stack);
    }

    /// Primitive (non-boxing) stack setter. Implementors may override for efficiency.
    default void setFloatValue(ItemStack stack, float value) {
        setValue(stack, value);
    }

    interface FloatMetaBlockProperty extends FloatBlockProperty, MetaBlockProperty<Float> {

        int getMetaPrimitive(float value, int existing);

        float getValuePrimitive(int meta);

        @Override
        default int getMeta(Float value, int existing) {
            return getMetaPrimitive(value, existing);
        }

        @Override
        default Float getValue(int meta) {
            return getValuePrimitive(meta);
        }

        @Override
        default float getFloatValue(IBlockAccess world, int x, int y, int z) {
            return getValuePrimitive(world.getBlockMetadata(x, y, z));
        }

        @Override
        default void setFloatValue(World world, int x, int y, int z, float value) {
            int meta = needsExisting() ? world.getBlockMetadata(x, y, z) : 0;

            int newMeta = getMetaPrimitive(value, meta);

            if (newMeta != meta) {
                world.setBlockMetadataWithNotify(x, y, z, newMeta, 2);
            }
        }

        @Override
        default float getFloatValue(ItemStack stack) {
            return getValuePrimitive(stack.getItem().getMetadata(stack.getItemDamage()));
        }

        @Override
        default void setFloatValue(ItemStack stack, float value) {
            int meta = needsExisting() ? stack.getItem().getMetadata(stack.getItemDamage()) : 0;

            int newMeta = getMetaPrimitive(value, meta);

            Items.feather.setDamage(stack, newMeta);
        }
    }

}
