package com.gtnewhorizon.gtnhlib.api.thaumcraft;

import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

import java.util.Map;

public class EnhancedInfusionRecipe extends InfusionRecipe  {
    private Map<ItemStack, ItemStack> replacements;

    public EnhancedInfusionRecipe(String research, Object output, int inst, AspectList aspects2, ItemStack input, ItemStack[] recipe, Map<ItemStack, ItemStack> replacements) {
        super(research, output, inst, aspects2, input, recipe);
        this.replacements = replacements;
    }


    public boolean hasReplacement (ItemStack key) {
        return this.replacements.containsKey(key);
    }


    public ItemStack getReplacement( ItemStack key) {
        return this.replacements.get(key);
    }

}
