package com.gtnewhorizon.gtnhlib.api.thaumcraft;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;

public class EnhancedInfusionRecipe extends InfusionRecipe {

    private final List<Replacement> replacements;

    // A guard value only returned when no replacement is found for an item
    public static final Replacement NO_REPLACEMENT = new Replacement(null, null, true);

    /**
     * Create a new EnhancedInfusionRecipe, capable of replacing items on the outer pedestals with other items rather
     * than consuming them or leaving their containers behind. An example use case is Witching Gadgets' Primordial Armor
     * recipes leaving the Inert Primordial Pearls on the outer pedestals in place of Primordial Pearls.
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

    /**
     * Automatically create and register an Enhanced Infusion Recipe, which is then returned for use, for example in a
     * research page
     * 
     * @param research     The required research for this infusion
     * @param output       The item created by this infusion
     * @param inst         The instability of this infusion
     * @param aspects2     The required essentia for this infusion
     * @param input        The item required to be placed on the central pedestal
     * @param recipe       The items required to be placed on the outer pedestals
     * @param replacements What items to replace consumed items with on the outer pedestals
     * @return The Enhanced infusion recipe created
     */

    public static EnhancedInfusionRecipe addEnhancedInfusionCraftingRecipe(String research, Object output, int inst,
            AspectList aspects2, ItemStack input, ItemStack[] recipe, List<Replacement> replacements) {
        if (!(output instanceof ItemStack) && !(output instanceof Object[])) {
            return null;
        } else {
            EnhancedInfusionRecipe r = new EnhancedInfusionRecipe(
                    research,
                    output,
                    inst,
                    aspects2,
                    input,
                    recipe,
                    replacements);
            ThaumcraftApi.getCraftingRecipes().add(r);
            return r;
        }
    }

    public List<Replacement> getReplacements() {
        return this.replacements;
    }

    /**
     * A Replacement defines one type of ItemStack (e.g. all Iron Ingots) to replace with another ItemStack (e.g.
     * Diamonds). Setting strict to false allows input to match with all metadata if input is given metadata of
     * Short.MAX_VALUE. NBT data (such as enchantments) is ignored when comparing against input. The output may be null
     * (e.g. for water buckets and other containers)
     *
     * @param input  The input ItemStack to compare recipe ingredients against
     * @param output The ItemStack to replace items that match the input
     * @param strict Whether to use strict mode in OreDictionary.itemMatches while comparing recipe ingredients to input
     */
    @Desugar
    public record Replacement(ItemStack input, ItemStack output, boolean strict) {}
}
