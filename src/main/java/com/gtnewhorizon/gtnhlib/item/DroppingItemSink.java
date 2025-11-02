package com.gtnewhorizon.gtnhlib.item;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockpos.IBlockPos;
import com.gtnewhorizon.gtnhlib.capability.item.ItemSink;

public class DroppingItemSink implements ItemSink {

    private final World world;
    private final IBlockPos pos;

    public DroppingItemSink(World world, IBlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    @Override
    public int store(ImmutableItemStack stack) {
        EntityItem entity = new EntityItem(
                world,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                stack.toStack());

        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;

        world.spawnEntityInWorld(entity);

        return 0;
    }
}
