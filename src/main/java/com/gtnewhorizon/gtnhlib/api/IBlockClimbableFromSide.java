package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * A block that an entity climbs as a ladder when it touches a side of the block.
 */
public interface IBlockClimbableFromSide {

    /**
     * Tells if an entity can climb this block.
     *
     * @param entity the entity that touches the block
     * @return true if the entity can climb the block
     */
    default boolean canClimb(EntityLivingBase entity) {
        return true;
    }

    /**
     * Tells if a side of this block can be climbed.
     *
     * @param world the world the block is in
     * @param x     the x coordinate of the block
     * @param y     the y coordinate of the block
     * @param z     the z coordinate of the block
     * @param side  the side of the block that the entity touches
     * @return true if the side can be climbed
     */
    default boolean isClimbableSide(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return true;
    }
}
