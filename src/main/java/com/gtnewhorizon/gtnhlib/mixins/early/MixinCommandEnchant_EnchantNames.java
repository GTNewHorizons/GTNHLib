package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.command.CommandEnchant;
import net.minecraft.command.ICommandSender;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.gtnhlib.modern.ModernIds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(CommandEnchant.class)
public class MixinCommandEnchant_EnchantNames {

    @WrapOperation(
            method = "processCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/command/CommandEnchant;parseIntBounded(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;II)I",
                    ordinal = 0))
    private int gtnhlib$namedEnchantment(ICommandSender sender, String text, int min, int max,
            Operation<Integer> original) {
        var enchantment = ModernIds.getEnchantment(text);

        if (enchantment != null) {
            return enchantment.effectId;
        }

        return original.call(sender, text, min, max);
    }
}
