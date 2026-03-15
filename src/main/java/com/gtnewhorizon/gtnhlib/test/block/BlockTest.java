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

public class BlockTest extends Block {

    /// This default blockstate
    private final AbstractDirectionBlockProperty FACING_PROP = (AbstractDirectionBlockProperty) DirectionBlockProperty
            .facing();

    public BlockTest() {
        super(Material.wood);
    }

    public static void register() {
        final var testBlock = new BlockTest();
        GameRegistry.registerBlock(testBlock, "model_test");

        /// This method allows you to adapt any property to itemstacks, with some default value. This is mostly relevant
        /// for modeled blocks, as they may require some state to match *any* of their models. If you're defining a
        /// custom blockstate, it's better to add an ItemStack default yourself - see [BlockTestTintMul] for an example
        /// of that.
        BlockPropertyRegistry.registerBlockItemProperty(testBlock, testBlock.FACING_PROP, ForgeDirection.EAST);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, int x, int y, int z, EntityLivingBase placer, ItemStack itemIn) {
        var dir = DirectionUtil.yawToDirection(placer.rotationYaw);

        /// This is a shortcut that works because this block only has a single meta property. See [BlockTestTintMul] for
        /// an example with non-meta blockstate.
        worldIn.setBlockMetadataWithNotify(x, y, z, FACING_PROP.getMeta(dir, 0), 2);
    }
}
