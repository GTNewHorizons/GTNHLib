package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.command.CommandEffect;
import net.minecraft.command.ICommandSender;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.modern.ModernIds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(CommandEffect.class)
public class MixinCommandEffect_PotionNames {

    @WrapOperation(
            method = "processCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/command/CommandEffect;parseIntWithMin(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;I)I"))
    private int gtnhlib$namedPotions(ICommandSender sender, String text, int min, Operation<Integer> original) {
        var potion = ModernIds.getPotion(text);

        if (potion != null) {
            return potion.id;
        }

        return original.call(sender, text, min);
    }
}
