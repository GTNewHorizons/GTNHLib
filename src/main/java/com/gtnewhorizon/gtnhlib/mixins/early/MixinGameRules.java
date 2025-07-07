package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.gamerules.GameRuleRegistry;
import com.gtnewhorizon.gtnhlib.mixins.interfaces.IGameRulesMixin;

@Mixin(GameRules.class)
public abstract class MixinGameRules implements IGameRulesMixin {

    @Unique
    private World gtnhlib$world;

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
        GameRuleRegistry.notifyGameRuleUpdate(ruleName, value, gtnhlib$world);
    }

    @Override
    public World gtnhlib$getWorld() {
        return this.gtnhlib$world;
    }

    @Override
    public void gtnhlib$setWorld(World world) {
        this.gtnhlib$world = world;
    }

}
