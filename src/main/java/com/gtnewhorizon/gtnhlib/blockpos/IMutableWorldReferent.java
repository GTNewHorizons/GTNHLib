package com.gtnewhorizon.gtnhlib.blockpos;

import net.minecraft.world.World;

public interface IMutableWorldReferent extends IWorldReferent {

    void setWorld(World world);
}
