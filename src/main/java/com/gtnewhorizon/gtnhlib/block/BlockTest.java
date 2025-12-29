package com.gtnewhorizon.gtnhlib.block;

import static com.gtnewhorizon.gtnhlib.client.model.ModelISBRH.JSON_ISBRH_ID;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockTest extends Block {

    public BlockTest() {
        super(Material.wood);
        setBlockName("model_test");
        // wood stats so it doesn't instabreak
        setHardness(2.0F);
        setResistance(5.0F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, int x, int y, int z, EntityLivingBase placer, ItemStack itemIn) {
        int meta = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        worldIn.setBlockMetadataWithNotify(x, y, z, meta, 2);
    }

    @Override
    public int getRenderType() {
        return JSON_ISBRH_ID;
    }
}
