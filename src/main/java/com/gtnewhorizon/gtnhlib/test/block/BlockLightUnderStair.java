package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty.AbstractDirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.util.DirectionUtil;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockLightUnderStair extends Block {

    public final AbstractDirectionBlockProperty FACING_PROP = (AbstractDirectionBlockProperty) DirectionBlockProperty
            .facing();

    public BlockLightUnderStair() {
        super(Material.wood);
    }

    public static void register() {
        final var testBlock = new BlockLightUnderStair();
        testBlock.setBlockName("light_under_stair");
        GameRegistry.registerBlock(testBlock, "light_under_stair");

        BlockPropertyRegistry.registerBlockItemProperty(testBlock, testBlock.FACING_PROP, ForgeDirection.EAST);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, int x, int y, int z, EntityLivingBase placer, ItemStack itemIn) {
        var dir = DirectionUtil.yawToDirection(placer.rotationYaw);

        worldIn.setBlockMetadataWithNotify(x, y, z, FACING_PROP.getMeta(dir, 0), 2);
    }
}
