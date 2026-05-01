package com.gtnewhorizon.gtnhlib.mixins.early;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.world.storage.SaveFormatComparator;
import net.minecraftforge.common.MinecraftForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.client.event.WorldDeletionEvent;

@Mixin(value = GuiSelectWorld.class)
public class MixinGuiSelectWorld extends GuiScreen {

    @Shadow
    private List field_146639_s;

    @Inject(method = "confirmClicked", at = @At("HEAD"))
    public void onConfirmClicked(boolean result, int id, CallbackInfo ci) {
        String fileName = ((SaveFormatComparator) this.field_146639_s.get(id)).getFileName();
        MinecraftForge.EVENT_BUS.post(new WorldDeletionEvent(fileName));
    }
}
