package com.gtnewhorizon.gtnhlib.mixins.preinit;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.gtnhlib.api.gui.WCWHelper;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;

import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.registry.GameData;

@Mixin(GameData.class)
public class MixinGameData_WorldConversionWarning {

    // This pre-init mixin is required to inject into the exact moment after missing mapping events are fired
    // but before the confirmation message appears.
    @Definition(id = "defaulted", local = @Local(type = List.class, name = "defaulted"))
    @Definition(id = "isEmpty", method = "Ljava/util/List;isEmpty()Z")
    @Expression("defaulted.isEmpty()")
    @Inject(
            method = "processIdRematches",
            at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.BY, by = -2),
            remap = false)
    private static void gtnhlib$processIdRematches(Iterable<FMLMissingMappingsEvent.MissingMapping> missedMappings,
            boolean isLocalWorld, GameData gameData, Map<String, Integer[]> remaps,
            CallbackInfoReturnable<List<String>> cir) {
        WCWHelper.fireWarnings();
    }
}
