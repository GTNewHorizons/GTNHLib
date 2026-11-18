package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * A block that an entity climbs as a ladder when it touches a side of the block.
 */
public interface IBlockClimbableFromSide {

    /**
     * Tells if this player entity can climb at this side here
     *
     * @param world        the world the block is in
     * @param x            the x coordinate of the block
     * @param y            the y coordinate of the block
     * @param z            the z coordinate of the block
     * @param side         the side of the block that the entity touches
     * @param entityPlayer the player entity that tries to climb
     * @return true if the side can be climbed
     * @implNote Keep this method lean: it runs for each entity that tries to climb.
     */
    @SuppressWarnings({ "unused", "MethodWithTooManyParameters" }) // default method by design
    default boolean isClimbableSide(IBlockAccess world, int x, int y, int z, ForgeDirection side,
            EntityPlayer entityPlayer) {
        return true;
    }
}
