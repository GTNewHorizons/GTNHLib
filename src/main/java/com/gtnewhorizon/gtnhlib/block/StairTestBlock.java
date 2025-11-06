package com.gtnewhorizon.gtnhlib.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;
import static java.lang.Math.max;
import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.VALID_DIRECTIONS;
import static net.minecraftforge.common.util.ForgeDirection.getOrientation;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

public class StairTestBlock extends Block {

    public StairTestBlock() {
        super(Material.wood);
        // it's wood
        setHardness(2.0F);
        setResistance(5.0F);
        setLightOpacity(255);
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
    public int onBlockPlaced(@NotNull World worldIn, int x, int y, int z, int side, float subX, float subY, float subZ,
            int meta) {

        // Face NORTH if placed up or down
        final var s = getOrientation(side);
        if (s == UP || s == DOWN) return 4;

        // Face the placed side
        return side;
    }

    @Override
    public int getMixedBrightnessForBlock(IBlockAccess worldIn, int x, int y, int z) {
        final int meta = worldIn.getBlockMetadata(x, y, z);
        int blockLight = 0;
        int skyLight = 0;

        // This is a stair, so take the light from adjacents in every direction but the back and underneath.
        // Also reduce the value by one, for attenuation.
        for (var dir : VALID_DIRECTIONS) {
            if (dir == DOWN || dir == getOrientation(meta).getOpposite()) continue;

            var lm = worldIn.getLightBrightnessForSkyBlocks(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, 0);
            blockLight = max(blockLight, (lm >> 4 & 0xFF) - 1);
            skyLight = max(skyLight, (lm >> 20 & 0xFF) - 1);
        }

        return skyLight << 20 | blockLight << 4;
    }

    @Override
    public int getRenderType() {
        return JSON_ISBRH_ID;
    }
}
