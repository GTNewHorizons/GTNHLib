package com.gtnewhorizon.gtnhlib.test.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.blockstate.properties.DirectionBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.registry.BlockPropertyRegistry;

import cpw.mods.fml.common.registry.GameRegistry;

public class BlockTest extends Block {

    private final DirectionBlockProperty.AbstractDirectionBlockProperty FACING_PROP = (DirectionBlockProperty.AbstractDirectionBlockProperty) DirectionBlockProperty
            .facing();

    public BlockTest() {
        super(Material.wood);
    }

    public static void register() {
        final var testBlock = new BlockTest();
        GameRegistry.registerBlock(testBlock, "model_test");

        BlockPropertyRegistry.registerProperty(testBlock, testBlock.FACING_PROP);
        BlockPropertyRegistry.registerProperty(Item.getItemFromBlock(testBlock), testBlock.FACING_PROP);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    public static float mod(float a, float b) {
        if (a < 0) {
            return b - (-a % b);
        }
        return a % b;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, int x, int y, int z, EntityLivingBase placer, ItemStack itemIn) {
        int direction = Math.round(mod(placer.rotationYaw, 360) / 360 * 4);
        var dir = switch (direction) {
            case 1 -> ForgeDirection.EAST;
            case 2 -> ForgeDirection.SOUTH;
            case 3 -> ForgeDirection.WEST;
            default -> ForgeDirection.NORTH;
        };

        worldIn.setBlockMetadataWithNotify(x, y, z, FACING_PROP.getMeta(dir, 0), 2);
    }

    @Override
    public int getRenderType() {
        return JSON_ISBRH_ID;
    }
}
