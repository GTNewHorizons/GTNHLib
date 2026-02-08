package com.gtnewhorizon.gtnhlib.test.item;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.gtnhlib.color.RGBColor;
import com.gtnewhorizon.gtnhlib.itemrendering.BlockItemTexture;
import com.gtnewhorizon.gtnhlib.itemrendering.IItemTexture;
import com.gtnewhorizon.gtnhlib.itemrendering.ItemTexture;
import com.gtnewhorizon.gtnhlib.itemrendering.ItemWithTextures;

public class TestItem extends Item implements ItemWithTextures {

    public static final TestItem INSTANCE = new TestItem();

    public TestItem() {
        setUnlocalizedName("testitem");
    }

    @Override
    public IItemTexture[] getTextures(ItemStack stack) {
        return new IItemTexture[] { new BlockItemTexture(Blocks.dirt.getIcon(0, 0), RGBColor.WHITE),
                new ItemTexture(Items.diamond_pickaxe.getIconFromDamage(0), RGBColor.WHITE), };
    }
}
