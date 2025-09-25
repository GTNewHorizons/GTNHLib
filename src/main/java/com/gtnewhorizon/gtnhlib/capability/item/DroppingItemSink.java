package com.gtnewhorizon.gtnhlib.capability.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

import appeng.api.util.DimensionalCoord;

public class DroppingItemSink implements IItemSink {

    private final DimensionalCoord dropLocation;

    public DroppingItemSink(DimensionalCoord dropLocation) {
        this.dropLocation = dropLocation;
    }

    @Override
    public ItemStack store(ItemStack stack) {
        EntityItem entity = new EntityItem(
                dropLocation.getWorld(),
                dropLocation.x + 0.5,
                dropLocation.y + 0.5,
                dropLocation.z + 0.5,
                stack);
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
        dropLocation.getWorld().spawnEntityInWorld(entity);

        return null;
    }
}
