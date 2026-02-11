package com.gtnewhorizon.gtnhlib.api.thaumcraft;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.github.bsideup.jabel.Desugar;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

public class EnhancedInfusionRecipe extends InfusionRecipe {

    private List<ReplacementMap> replacements;

    public EnhancedInfusionRecipe(String research, Object output, int inst, AspectList aspects2, ItemStack input,
            ItemStack[] recipe, List<ReplacementMap> replacements) {
        super(research, output, inst, aspects2, input, recipe);
        this.replacements = replacements;
    }

    public ItemStack getReplacement(ItemStack key) {
        for (ReplacementMap replacement : this.replacements) {
            if (OreDictionary.itemMatches(replacement.input, key, replacement.strict)) {
                return replacement.output;
            }
        }
        return null;
    }

    public List<ReplacementMap> getReplacementMap() {
        return this.replacements;
    }

    public boolean isRepEmpty() {
        return this.replacements.isEmpty();
    }

    @Desugar
    public record ReplacementMap(ItemStack input, ItemStack output, boolean strict) {}
}
