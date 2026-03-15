package com.gtnewhorizon.gtnhlib.test.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.color.IBlockColor;
import com.gtnewhorizon.gtnhlib.util.DirectionUtil;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockTestTintMul extends Block implements IBlockColor {

    public BlockTestTintMul() {
        super(Material.wood);
        setHardness(2f);
    }

    public static void register() {
        final var tintMulTestBlock = new BlockTestTintMul();
        GameRegistry.registerBlock(tintMulTestBlock, "model_test_tint_mul");
        GameRegistry.registerTileEntity(TileTestTintMul.class, "tile_test_tint_mul");

        // Here's an example of a custom, TE-based property
        final var property = new DirectionBlockProperty() {

            @Override
            public String getName() {
                return "facing";
            }

            /// This DOES support stacks, otherwise the item wouldn't have a facing, and thus wouldn't have a model.
            @Override
            public boolean hasTrait(BlockPropertyTrait trait) {
                return switch (trait) {
                    case SupportsWorld, WorldMutable, StackMutable, SupportsStacks -> true;
                    default -> false;
                };
            }

            @Override
            public ForgeDirection getValue(IBlockAccess world, int x, int y, int z) {
                TileEntity te = world.getTileEntity(x, y, z);
                if (te instanceof TileTestTintMul tile) return tile.getFacing();
                return ForgeDirection.NORTH;
            }

            @Override
            public void setValue(World world, int x, int y, int z, ForgeDirection value) {
                TileEntity te = world.getTileEntity(x, y, z);
                if (te instanceof TileTestTintMul tile) tile.setFacing(value);
            }

            /// This needs to be implemented for the stack support to work.
            @Override
            public ForgeDirection getValue(ItemStack stack) {
                return ForgeDirection.NORTH;
            }
        };

        BlockPropertyRegistry.registerBlockItemProperty(tintMulTestBlock, property);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemIn) {
        BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z);
        state.setPropertyValue("facing", DirectionUtil.yawToDirection(player.rotationYaw));
        state.place(world, x, y, z);
        state.close();
    }

    @Override
    public boolean hasTileEntity(int meta) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta) {
        return new TileTestTintMul();
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        return switch (tintIndex) {
            case 0 -> 0xFF0000; // red
            case 1 -> 0x00FF00; // green
            case 2 -> 0x0000FF; // blue
            case 3 -> 0xFFFF00; // yellow
            case 4 -> 0xFF00FF; // purple
            case 5 -> 0x00FFFF; // cyan
            default -> 0xFFFFFF;// white
        };
    }

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Like block
        return colorMultiplier(null, 0, 0, 0, tintIndex);
    }
}
