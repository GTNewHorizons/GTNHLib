package com.gtnewhorizon.gtnhlib.config;

import java.util.Set;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface SimpleGuiFactory extends IModGuiFactory {

    @Override
    default void initialize(Minecraft minecraftInstance) {

    }

    @Override
    default Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    default RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }
}
