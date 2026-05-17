package com.gtnewhorizon.gtnhlib.datacomponent.registry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.datacomponent.core.DataComponentType;

public interface DataComponentFactory<TValue> {

    @Nullable
    DataComponentType<TValue> getComponent(ItemStack stack, Item item, int meta);
}
