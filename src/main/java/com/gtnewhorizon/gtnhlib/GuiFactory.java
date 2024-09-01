package com.gtnewhorizon.gtnhlib;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;
import com.gtnewhorizon.gtnhlib.config.TestConfig;

public class GuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GuiTest.class;
    }

    public static class GuiTest extends SimpleGuiConfig {

        public GuiTest(GuiScreen parent) throws ConfigException {
            super(parent, GTNHLib.MODID, GTNHLib.MODNAME, true, TestConfig.class);
        }
    }
}
