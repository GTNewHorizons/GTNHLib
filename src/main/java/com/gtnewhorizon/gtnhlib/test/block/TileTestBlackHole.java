package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

public class TileTestBlackHole extends TileEntity {

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        final double size = BlackHoleSettings.radius;
        return AxisAlignedBB.getBoundingBox(
                xCoord + 0.5 - size,
                yCoord + 0.5 - size,
                zCoord + 0.5 - size,
                xCoord + 0.5 + size,
                yCoord + 0.5 + size,
                zCoord + 0.5 + size);
    }
}
