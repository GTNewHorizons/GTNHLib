package com.gtnewhorizon.gtnhlib.blockpos;

import java.util.Iterator;

import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;

import com.google.common.collect.AbstractIterator;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

public class BlockPos extends Vector3i implements IMutableBlockPos {

    public BlockPos() {
        super();
    }

    public BlockPos(int x, int y, int z) {
        super(x, y, z);
    }

    public BlockPos(ChunkPosition chunkPosition) {
        super(chunkPosition.chunkPosX, chunkPosition.chunkPosY, chunkPosition.chunkPosZ);
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getZ() {
        return this.z;
    }

    @Override
    public BlockPos offset(ForgeDirection d) {
        return new BlockPos(this.x + d.offsetX, this.y + d.offsetY, this.z + d.offsetZ);
    }

    @Override
    public BlockPos offset(int x, int y, int z) {
        return new BlockPos(this.x + x, this.y + y, this.z + z);
    }

    @Override
    public BlockPos down() {
        return offset(ForgeDirection.DOWN);
    }

    @Override
    public BlockPos up() {
        return offset(ForgeDirection.UP);
    }

    @Override
    public BlockPos copy() {
        return new BlockPos(this.x, this.y, this.z);
    }

    @Override
    public long asLong() {
        return CoordinatePacker.pack(this.x, this.y, this.z);
    }

    @Override
    public BlockPos set(int x, int y, int z) {
        super.set(x, y, z);
        return this;
    }

    @Override
    public BlockPos set(long packedPos) {
        CoordinatePacker.unpack(packedPos, this);
        return this;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /**
     * Returns an iterable of all BlockPos objects within the specified box. The range is inclusive.
     *
     * @param from the starting BlockPos of the box
     * @param to   the ending BlockPos of the box
     * @return an iterable of all BlockPos objects within the box
     */
    public static Iterable<BlockPos> getAllInBox(BlockPos from, BlockPos to) {
        return getAllInBox(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()),
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));
    }

    /**
     * Returns an iterable of BlockPos objects representing all positions within the specified box. The range is
     * inclusive.
     *
     * @param xMin The minimum x-coordinate of the box.
     * @param yMin The minimum y-coordinate of the box.
     * @param zMin The minimum z-coordinate of the box.
     * @param xMax The maximum x-coordinate of the box.
     * @param yMax The maximum y-coordinate of the box.
     * @param zMax The maximum z-coordinate of the box.
     * @return An iterable of BlockPos objects representing all positions within the specified box.
     */
    public static Iterable<BlockPos> getAllInBox(final int xMin, final int yMin, final int zMin, final int xMax,
            final int yMax, final int zMax) {
        return new Iterable<>() {

            public Iterator<BlockPos> iterator() {
                return new AbstractIterator<>() {

                    private BlockPos pos;

                    protected BlockPos computeNext() {
                        if (this.pos == null) {
                            this.pos = new BlockPos(xMin, yMin, zMin);
                            return this.pos;
                        } else if (this.pos.x == xMax && this.pos.y == yMax && this.pos.z == zMax) {
                            return this.endOfData();
                        } else {
                            if (this.pos.x < xMax) {
                                ++this.pos.x;
                            } else if (this.pos.y < yMax) {
                                this.pos.x = xMin;
                                ++this.pos.y;
                            } else if (this.pos.z < zMax) {
                                this.pos.x = xMin;
                                this.pos.y = yMin;
                                ++this.pos.z;
                            }

                            return this.pos;
                        }
                    }
                };
            }
        };
    }
}
