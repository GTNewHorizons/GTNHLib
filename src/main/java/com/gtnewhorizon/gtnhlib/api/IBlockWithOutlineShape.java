package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/**
 * A block whose selection outline and click area are an {@link OutlineShape}, by default its collision boxes. Implement
 * this on a {@link Block}. For example, a post on a base:
 *
 * <pre>
 * {@code
 * public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity e) {
 *     setBlockBounds(0, 0, 0, 1, 0.25F, 1);
 *     super.addCollisionBoxesToList(world, x, y, z, mask, list, e);
 *     setBlockBounds(0.25F, 0.25F, 0.25F, 0.75F, 1, 0.75F);
 *     super.addCollisionBoxesToList(world, x, y, z, mask, list, e);
 *     setBlockBounds(0, 0, 0, 1, 1, 1);
 * }
 * }
 * </pre>
 *
 * For a different outline, override {@link #getOutlineShape} and return a shape built once, such as
 * {@code OutlineShape.of(box1, box2)}. If the block overrides {@code collisionRayTrace}, it must call {@code super} for
 * the shape to be used.
 */
public interface IBlockWithOutlineShape {

    /// [OutlineShape#EMPTY] or null uses the block's own outline. Defaults to the boxes from `addCollisionBoxesToList`,
    /// called with a null entity.
    default OutlineShape getOutlineShape(World world, int x, int y, int z) {
        return OutlineShape.fromCollision((Block) this, world, x, y, z);
    }
}
