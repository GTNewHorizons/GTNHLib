package com.gtnewhorizon.gtnhlib.test.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.client.model.color.IBlockColor;

public class BlockTestTint extends Block implements IBlockColor {

    public BlockTestTint() {
        super(Material.wood);
        setHardness(2f);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return JSON_ISBRH_ID;
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
