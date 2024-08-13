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
    public IBlockPos offset(ForgeDirection d) {
        return new BlockPos(this.x + d.offsetX, this.y + d.offsetY, this.z + d.offsetZ);
    }

    @Override
    public IBlockPos down() {
        return offset(ForgeDirection.DOWN);
    }

    @Override
    public IBlockPos up() {
        return offset(ForgeDirection.UP);
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

    public static Iterable<BlockPos> getAllInBox(BlockPos from, BlockPos to) {
        return getAllInBox(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()),
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));
    }

    public static Iterable<BlockPos> getAllInBox(final int x1, final int y1, final int z1, final int x2, final int y2,
            final int z2) {
        return new Iterable<>() {

            public Iterator<BlockPos> iterator() {
                return new AbstractIterator<>() {

                    private BlockPos pos;

                    protected BlockPos computeNext() {
                        if (this.pos == null) {
                            this.pos = new BlockPos(x1, y1, z1);
                            return this.pos;
                        } else if (this.pos.x == x2 && this.pos.y == y2 && this.pos.z == z2) {
                            return this.endOfData();
                        } else {
                            if (this.pos.x < x2) {
                                ++this.pos.x;
                            } else if (this.pos.y < y2) {
                                this.pos.x = x1;
                                ++this.pos.y;
                            } else if (this.pos.z < z2) {
                                this.pos.x = x1;
                                this.pos.y = y1;
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
