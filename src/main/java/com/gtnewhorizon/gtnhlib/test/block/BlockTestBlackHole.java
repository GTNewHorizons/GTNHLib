package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockTestBlackHole extends Block {

    private BlockTestBlackHole() {
        super(Material.rock);
        setHardness(1f);
        setBlockTextureName("obsidian");
    }

    public static void register() {
        final BlockTestBlackHole block = new BlockTestBlackHole();
        block.setBlockName("model_test_black_hole");
        GameRegistry.registerBlock(block, "model_test_black_hole");
        GameRegistry.registerTileEntity(TileTestBlackHole.class, "tile_test_black_hole");
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileTestBlackHole();
    }
}
