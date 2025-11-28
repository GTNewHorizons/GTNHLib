package com.gtnewhorizon.gtnhlib.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
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

    @Override
    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
        return ModelISBRH.INSTANCE.addDestroyEffects(world, x, y, z, meta, effectRenderer);
    }

    @Override
    public boolean addHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer) {
        return ModelISBRH.INSTANCE.addHitEffects(worldObj, target, effectRenderer);
    }
}
