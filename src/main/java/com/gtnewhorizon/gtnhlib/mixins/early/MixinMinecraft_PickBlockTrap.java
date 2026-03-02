package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.event.PickBlockEvent;

@Mixin(Minecraft.class)
public class MixinMinecraft_PickBlockTrap {

    @Inject(method = "func_147112_ai", at = @At("HEAD"), cancellable = true)
    private void gt5u$before$pickBlock(CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new PickBlockEvent())) {
            ci.cancel();
        }
    }
}
