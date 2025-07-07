package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.mixins.interfaces.IGameRulesMixin;

@Mixin(World.class)
public class MixinWorld {

    @Shadow
    protected WorldInfo worldInfo;

    @Inject(
            method = {
                    "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/profiler/Profiler;)V",
                    "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/profiler/Profiler;)V" },
            at = @At("TAIL"))
    private void gtnhlib$onInit(CallbackInfo ci) {
        IGameRulesMixin gameRulesMixin = (IGameRulesMixin) worldInfo.getGameRulesInstance();
        gameRulesMixin.gtnhlib$setWorld((World) (Object) this);
    }
}
