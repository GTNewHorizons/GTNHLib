package com.gtnewhorizon.gtnhlib.blockpos;

import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3i;

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
}
