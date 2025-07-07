package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.gamerules.GameRuleRegistry;

@Mixin(GameRules.class)
public abstract class MixinGameRules {

    @Shadow
    public abstract void addGameRule(String p_82769_1_, String p_82769_2_);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtnhlib$initGameRules(CallbackInfo ci) {
        GameRuleRegistry.injectGameRules((GameRules) (Object) this);

    }

    @Inject(
            method = "setOrCreateGameRule",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/GameRules$Value;setValue(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER))
    private void GTNHLib$onGameRuleChanged(String ruleName, String value, CallbackInfo ci) {
        GameRuleRegistry.notifyGameRuleUpdate(ruleName, value);
    }
}
