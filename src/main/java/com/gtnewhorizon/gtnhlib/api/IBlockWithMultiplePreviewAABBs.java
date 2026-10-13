package com.gtnewhorizon.gtnhlib.api;

import java.util.List;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// Implement on a [net.minecraft.block.Block] to allow it to render multiple selection boxes when looked at by the
/// player. This is purely cosmetic and does not change the backing hitscan logic.
public interface IBlockWithMultiplePreviewAABBs {

    @SideOnly(Side.CLIENT)
    List<AxisAlignedBB> getPreviewAABBs(World world, int x, int y, int z);
}
