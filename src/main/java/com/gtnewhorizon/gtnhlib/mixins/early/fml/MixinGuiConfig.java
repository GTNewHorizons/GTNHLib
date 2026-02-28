package com.gtnewhorizon.gtnhlib.mixins.early.fml;

import java.util.Comparator;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.config.IConfigElementProxy;

import cpw.mods.fml.client.config.GuiConfig;

@Mixin(GuiConfig.class)
public abstract class MixinGuiConfig {

    @Inject(
            method = "<init>(Lnet/minecraft/client/gui/GuiScreen;Ljava/util/List;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcpw/mods/fml/client/config/GuiConfigEntries;<init>(Lcpw/mods/fml/client/config/GuiConfig;Lnet/minecraft/client/Minecraft;)V"))
    private void beforeGuiConfigEntries(GuiScreen parentScreen, List configElements, String modID, String configID,
            boolean allRequireWorldRestart, boolean allRequireMcRestart, String title, String titleLine2,
            CallbackInfo ci) {
        configElements.sort(
                Comparator.comparingInt(e -> e instanceof IConfigElementProxy<?>p ? p.getOrder() : Integer.MAX_VALUE));
    }
}
