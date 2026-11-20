package com.gtnewhorizon.gtnhlib.structure.nbt;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Marks spots {@link StructureSaver} leaves out, so placing the structure keeps whatever is already there. Registered
 * with {@link BlockStructureMarker}.
 */
public class BlockStructureVoid extends Block {

    public BlockStructureVoid() {
        super(Material.glass);
        this.setBlockName("gtnhlib.structure_void");
        this.setBlockTextureName("glass");
        this.setBlockUnbreakable();
        this.setResistance(6000000.0F);
        this.setBlockBounds(0.3F, 0.3F, 0.3F, 0.7F, 0.7F, 0.7F);
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    public static void register() {
        GameRegistry.registerBlock(new BlockStructureVoid(), "structure_void");
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
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }
}
