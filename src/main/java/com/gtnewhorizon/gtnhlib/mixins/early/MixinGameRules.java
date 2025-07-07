package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.gamerules.GameRuleHandler;

@Mixin(GameRules.class)
public class MixinGameRules {

    @Inject(method = "setOrCreateGameRule", at = @At("RETURN"))
    private void GTNHLib$onGameRuleChanged(String ruleName, String value, CallbackInfo ci) {
        if (GameRuleHandler.hasRule(ruleName)) {
            GameRuleHandler.updateCachedRule(ruleName, value);
        }
    }
}
