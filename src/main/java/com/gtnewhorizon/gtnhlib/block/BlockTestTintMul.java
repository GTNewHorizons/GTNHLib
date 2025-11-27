package com.gtnewhorizon.gtnhlib.block;

import com.gtnewhorizon.gtnhlib.client.model.color.IBlockColor;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.jetbrains.annotations.NotNull;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

public class BlockTestTintMul extends Block implements IBlockColor {

    public BlockTestTintMul() {
        super(Material.wood);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int onBlockPlaced(@NotNull World worldIn, int x, int y, int z, int side, float subX, float subY, float subZ,
            int meta) {

        // Face NORTH if placed up or down
        final var s = ForgeDirection.getOrientation(side);
        if (s == ForgeDirection.UP || s == ForgeDirection.DOWN) return 2;

        // Face the placed side
        return side - 2;
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
