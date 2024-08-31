package com.gtnewhorizon.gtnhlib.config;

import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SimpleGuiConfig extends GuiConfig {

    public SimpleGuiConfig(GuiScreen parent, Class<?> configClass, String modID, String modName)
            throws ConfigException {
        super(
                parent,
                ConfigurationManager.getConfigElements(configClass),
                modID,
                false,
                false,
                modName + " Configuration");
    }

    public SimpleGuiConfig(GuiScreen parent, String modID, String modName, Class<?>... configClasses)
            throws ConfigException {
        this(parent, modID, modName, false, configClasses);
    }

    public SimpleGuiConfig(GuiScreen parent, String modID, String modName, boolean categorized,
            Class<?>... configClasses) throws ConfigException {
        super(
                parent,
                ConfigurationManager.getConfigElementsMulti(categorized, configClasses),
                modID,
                false,
                false,
                modName + " Configuration");
    }
}
