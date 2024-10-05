package com.gtnewhorizon.gtnhlib.mixins.early.fml;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.llamalad7.mixinextras.sugar.Local;

import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.common.ModContainer;

@Mixin(GuiModList.class)
public abstract class MixinGuiModList extends GuiScreen {

    @Shadow(remap = false)
    private ModContainer selectedMod;

    @Shadow(remap = false)
    private GuiButton configModButton;

    @Inject(
            method = "actionPerformed",
            at = @At(
                    value = "INVOKE",
                    target = "Lcpw/mods/fml/client/IModGuiFactory;mainConfigGuiClass()Ljava/lang/Class;",
                    remap = false),
            cancellable = true)
    private void gtnhlib$autoCreateGuiConfig(GuiButton button, CallbackInfo ci, @Local IModGuiFactory guiFactory) {
        if (guiFactory == null && ConfigurationManager.isModRegistered(selectedMod.getModId())) {
            ci.cancel();
            try {
                GuiScreen config = new SimpleGuiConfig(this, selectedMod.getModId(), selectedMod.getName());
                mc.displayGuiScreen(config);
            } catch (ConfigException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Inject(
            method = "drawScreen",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/client/gui/GuiButton;enabled:Z",
                    shift = At.Shift.AFTER,
                    ordinal = 4))
    private void gtnhlib$checkForRegisteredConfig(int p_571_1_, int p_571_2_, float p_571_3_, CallbackInfo ci) {
        if (ConfigurationManager.isModRegistered(selectedMod.getModId())) {
            configModButton.enabled = true;
        }
    }
}
