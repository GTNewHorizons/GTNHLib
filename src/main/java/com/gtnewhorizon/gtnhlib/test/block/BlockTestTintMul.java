package com.gtnewhorizon.gtnhlib.test.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;
import com.gtnewhorizon.gtnhlib.client.model.color.IBlockColor;

public class BlockTestTintMul extends Block implements IBlockColor {

    public BlockTestTintMul() {
        super(Material.wood);
        setHardness(2f);
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
