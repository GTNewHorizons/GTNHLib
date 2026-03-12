package com.gtnewhorizon.gtnhlib.test.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.color.IBlockColor;

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
        final var property = new DirectionBlockProperty() {

            @Override
            public String getName() {
                return "facing";
            }

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
                if (te instanceof TileTestTintMul tile) {
                    return tile.getFacing();
                }
                return ForgeDirection.NORTH;
            }

            @Override
            public ForgeDirection getValue(Block block, int meta) {
                return ForgeDirection.NORTH;
            }

            @Override
            public void setValue(World world, int x, int y, int z, ForgeDirection value) {
                TileEntity te = world.getTileEntity(x, y, z);
                if (te instanceof TileTestTintMul tile) {
                    tile.setFacing(value);
                }
            }

            @Override
            public ForgeDirection getValue(ItemStack stack) {
                return ForgeDirection.NORTH;
            }
        };

        BlockPropertyRegistry.registerProperty(tintMulTestBlock, property);
        BlockPropertyRegistry.registerProperty(Item.getItemFromBlock(tintMulTestBlock), property);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemIn) {
        super.onBlockPlacedBy(world, x, y, z, player, itemIn);
        int heading = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        ForgeDirection facing = getDirectionForHeading(heading);
        BlockState state = BlockPropertyRegistry.getBlockState(world, x, y, z);
        state.setPropertyValue("facing", facing);
        state.place(world, x, y, z);
        state.close();
    }

    private ForgeDirection getDirectionForHeading(int heading) {
        return switch (heading) {
            case 0 -> ForgeDirection.NORTH;
            case 1 -> ForgeDirection.EAST;
            case 2 -> ForgeDirection.SOUTH;
            case 3 -> ForgeDirection.WEST;
            default -> ForgeDirection.NORTH;
        };
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
    public int getRenderType() {
        return JSON_ISBRH_ID;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        switch (tintIndex) {
            case 0:
                return 0xFF0000; // red
            case 1:
                return 0x00FF00; // green
            case 2:
                return 0x0000FF; // blue
            case 3:
                return 0xFFFF00; // yellow
            case 4:
                return 0xFF00FF; // purple
            case 5:
                return 0x00FFFF; // cyan
            default:
                return 0xFFFFFF;// white
        }
    }

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        // Like block
        return colorMultiplier(null, 0, 0, 0, tintIndex);
    }
}
