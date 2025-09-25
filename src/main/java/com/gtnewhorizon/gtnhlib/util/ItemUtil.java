package com.gtnewhorizon.gtnhlib.util;

import java.util.Objects;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Contract;

public class ItemUtil {

    public static int getStackMeta(ItemStack stack) {
        return Items.feather.getDamage(stack);
    }

    public static boolean isStackEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize <= 0;
    }

    public static boolean isStackInvalid(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize < 0;
    }

    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2) {
        return areStacksEqual(stack1, stack2, false);
    }

    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2, boolean ignoreNBT) {
        return stack1 != null && stack2 != null
                && stack1.getItem() == stack2.getItem()
                && doStackMetasMatch(getStackMeta(stack1), getStackMeta(stack2))
                && (ignoreNBT || Objects.equals(stack1.getTagCompound(), stack2.getTagCompound()));
    }

    public static boolean doStackMetasMatch(int meta1, int meta2) {
        if (meta1 == OreDictionary.WILDCARD_VALUE) return true;
        if (meta2 == OreDictionary.WILDCARD_VALUE) return true;

        return meta1 == meta2;
    }

    @Contract("_, null -> null")
    public static ItemStack copyAmount(int amount, ItemStack stack) {
        if (isStackInvalid(stack)) return null;

        stack = stack.copy();
        stack.stackSize = amount;

        return stack;
    }
}
