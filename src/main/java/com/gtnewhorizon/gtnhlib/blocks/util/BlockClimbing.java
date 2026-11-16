package com.gtnewhorizon.gtnhlib.blocks.util;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.api.IBlockClimbableFromSide;

/**
 * Lets entities climb the sides of {@link IBlockClimbableFromSide} blocks as ladders.
 */
public final class BlockClimbing {

    // Max gap between the entity and a block side for the side to count as touched.
    private static final double CONTACT_DISTANCE = 0.125D;

    private BlockClimbing() {}

    /**
     * Tells if an entity climbs a block side it touches.
     *
     * @param world  the world the entity is in
     * @param entity the entity to check
     * @return true if the entity climbs a block side
     */
    public static boolean isClimbing(World world, EntityLivingBase entity) {
        final AxisAlignedBB box = entity.boundingBox;
        final int y = MathHelper.floor_double(box.minY);
        final int minX = MathHelper.floor_double(box.minX - CONTACT_DISTANCE);
        final int maxX = MathHelper.floor_double(box.maxX + CONTACT_DISTANCE);
        final int minZ = MathHelper.floor_double(box.minZ - CONTACT_DISTANCE);
        final int maxZ = MathHelper.floor_double(box.maxZ + CONTACT_DISTANCE);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final ForgeDirection side = getTouchedSide(box, x, z);
                if (side != ForgeDirection.UNKNOWN && isClimbableSide(world, x, y, z, side, entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ForgeDirection getTouchedSide(AxisAlignedBB box, int x, int z) {
        final boolean overlapsX = box.maxX > x && box.minX < x + 1;
        final boolean overlapsZ = box.maxZ > z && box.minZ < z + 1;
        if (overlapsZ && !overlapsX) {
            return box.minX >= x + 1 ? ForgeDirection.EAST : ForgeDirection.WEST;
        }
        if (overlapsX && !overlapsZ) {
            return box.minZ >= z + 1 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
        }
        return ForgeDirection.UNKNOWN;
    }

    private static boolean isClimbableSide(World world, int x, int y, int z, ForgeDirection side,
            EntityLivingBase entity) {
        final Block block = world.getBlock(x, y, z);
        return block instanceof IBlockClimbableFromSide climbable && climbable.canClimb(entity)
                && climbable.isClimbableSide(world, x, y, z, side);
    }
}
