package com.gtnewhorizon.gtnhlib.client;

import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.registry.VillagerRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

// Moved from wawla's net.darkhax.wawla.util.Utilities
public class VillagerNames {

    /**
     * Lists of names for the vanilla villagers.
     */
    @SideOnly(Side.CLIENT)
    private static String[] vanillaVillagers = { "farmer", "librarian", "priest", "blacksmith", "butcher" };

    /**
     * Retrieves a unique string related to the texture name of a villager. This allows for villagers to be
     * differentiated based on their profession rather than their ID.
     *
     * @param id : The ID of the villager being looked up.
     * @return String: The texture name, minus file path and extension.
     */
    @SideOnly(Side.CLIENT)
    public static String getVillagerName(int id) {

        ResourceLocation skin = VillagerRegistry.getVillagerSkin(id, null);

        if (id >= 0 && id <= 4) {
            return vanillaVillagers[id];
        }

        if (skin != null) {
            return skin.getResourceDomain() + "."
                    + skin.getResourcePath().substring(
                            skin.getResourcePath().lastIndexOf("/") + 1,
                            skin.getResourcePath().length() - 4);
        }

        return "misingno";
    }
}
