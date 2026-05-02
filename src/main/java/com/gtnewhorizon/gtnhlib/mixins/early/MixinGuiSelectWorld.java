package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.common.MinecraftForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.event.WorldDeletionEvent;

@Mixin(value = GuiSelectWorld.class)
public class MixinGuiSelectWorld extends GuiScreen {

    @ModifyArg(
            method = "confirmClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/storage/ISaveFormat;deleteWorldDirectory(Ljava/lang/String;)Z"),
            index = 0)
    private String onDeleteWorldDirectoryCalled(String worldName) {
        try {
            MinecraftForge.EVENT_BUS.post(new WorldDeletionEvent(worldName));
        } catch (Throwable t) {
            GTNHLib.LOG.error("Exception while posting WorldDeletionEvent", t);
        }
        return worldName;
    }
}
