package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public interface BlockWithCustomSound {

    Block.SoundType getSound(World world, int x, int y, int z);
}
