package com.gtnewhorizon.gtnhlib.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

public class BlockTest extends Block {

    public BlockTest() {
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
}
