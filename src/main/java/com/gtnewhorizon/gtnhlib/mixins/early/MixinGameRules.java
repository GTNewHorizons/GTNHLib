package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.gamerules.GameRuleHandler;
import com.gtnewhorizon.gtnhlib.gamerules.IGameRule;

@Mixin(GameRules.class)
public abstract class MixinGameRules {

    @Shadow
    public abstract void addGameRule(String p_82769_1_, String p_82769_2_);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtnhlib$initGameRules(CallbackInfo ci) {
        for (IGameRule rule : GameRuleHandler.getGameRulesMap().values()) {
            this.addGameRule(rule.getName(), rule.defaultValue());
        }
    }

    @Inject(method = "setOrCreateGameRule", at = @At("RETURN"))
    private void GTNHLib$onGameRuleChanged(String ruleName, String value, CallbackInfo ci) {
        GameRuleHandler.notifyGameRuleUpdate(ruleName, value);
    }
}
