package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/// Implement this interface on a [Block] to make its sound dependent on some world state (meta, tile entities,
/// etc). This may be run on the server, so SideOnly(CLIENT) code cannot be used in this method.
public interface BlockWithCustomSound {

    Block.SoundType getSound(World world, int x, int y, int z);
}
