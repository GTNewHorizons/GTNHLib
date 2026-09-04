package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.util.DirectionUtil;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockRotationTest extends Block {

    public final DirectionBlockProperty.AbstractDirectionBlockProperty FACING_PROP = (DirectionBlockProperty.AbstractDirectionBlockProperty) DirectionBlockProperty
            .facing();

    public BlockRotationTest() {
        super(Material.iron);
    }

    public static void register(BlockRotationTest testBlock, String name) {
        testBlock.setBlockName(name);
        GameRegistry.registerBlock(testBlock, name);
        BlockPropertyRegistry.registerBlockItemProperty(testBlock, testBlock.FACING_PROP, ForgeDirection.EAST);
    }

    // Just have all test blocks use yaw to test things. We can rotate different ways either way.
    @Override
    public void onBlockPlacedBy(World worldIn, int x, int y, int z, EntityLivingBase placer, ItemStack itemIn) {
        var dir = DirectionUtil.yawToDirection(placer.rotationYaw);
        worldIn.setBlockMetadataWithNotify(x, y, z, FACING_PROP.getMeta(dir, 0), 2);
    }
}
