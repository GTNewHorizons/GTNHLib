package com.gtnewhorizon.gtnhlib.item.impl;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;
import com.gtnewhorizon.gtnhlib.item.ImmutableItemStack;

import cofh.api.transport.IItemDuct;

public class ItemDuctSink implements ItemSink {

    public final IItemDuct itemDuct;
    private final ForgeDirection side;

    public ItemDuctSink(IItemDuct itemDuct, ForgeDirection side) {
        this.itemDuct = itemDuct;
        this.side = side;
    }

    @Override
    public int store(ImmutableItemStack stack) {
        ItemStack rejected = itemDuct.insertItem(side, stack.toStack());

        if (rejected == null) return 0;

        if (!stack.matches(rejected)) {
            GTNHLib.LOG.error(
                    "IItemDuct returned a rejected item that does not match what was inserted: deleting it to prevent dupes ({}x{})",
                    rejected.stackSize,
                    rejected.getDisplayName(),
                    new Exception());

            return 0;
        }

        return rejected.stackSize;
    }
}
