package com.gtnewhorizon.gtnhlib.api.thaumcraft;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.github.bsideup.jabel.Desugar;

import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

public class EnhancedInfusionRecipe extends InfusionRecipe {

    private final List<Replacement> replacements;
    public static final Replacement NO_REPLACEMENT = new Replacement(null, null, true);

    /**
     * Create a new EnhancedInfusionRecipe, capable of
     *
     * @param research     The required research for this infusion
     * @param output       The item created by this infusion
     * @param inst         The instability of this infusion
     * @param aspects2     The required essentia for this infusion
     * @param input        The item required to be placed on the central pedestal
     * @param recipe       The items required to be placed on the outer pedestals
     * @param replacements What items to replace consumed items with on the outer pedestals
     */
    public EnhancedInfusionRecipe(String research, Object output, int inst, AspectList aspects2, ItemStack input,
            ItemStack[] recipe, List<Replacement> replacements) {
        super(research, output, inst, aspects2, input, recipe);
        this.replacements = replacements;
    }

    public ItemStack getReplacement(ItemStack key) {
        for (Replacement replacement : this.replacements) {
            if (OreDictionary.itemMatches(replacement.input, key, replacement.strict)) {
                return replacement.output;
            }
        }
        return null;
    }

    public List<Replacement> getReplacementMap() {
        return this.replacements;
    }

    public boolean isRepEmpty() {
        return this.replacements.isEmpty();
    }

    @Desugar
    public record Replacement(ItemStack input, ItemStack output, boolean strict) {}
}
