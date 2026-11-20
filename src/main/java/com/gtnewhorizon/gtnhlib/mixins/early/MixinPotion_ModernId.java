package com.gtnewhorizon.gtnhlib.mixins.early;

import java.util.Locale;

import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.modern.ModernIdExt;
import com.gtnewhorizon.gtnhlib.modern.ModernIds;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import lombok.SneakyThrows;

@Mixin(Potion.class)
public abstract class MixinPotion_ModernId implements ModernIdExt {

    @Shadow
    public abstract String getName();

    @Unique
    private String gtnhlib$owningMod;

    @SneakyThrows
    @Inject(method = "<init>", at = @At("TAIL"))
    private void gtnhlib$interceptMod(int p_i1573_1_, boolean p_i1573_2_, int p_i1573_3_, CallbackInfo ci) {
        var modController = Loader.class.getDeclaredField("modController");
        modController.setAccessible(true);
        LoadController loadController = (LoadController) modController.get(Loader.instance());

        gtnhlib$owningMod = loadController == null || loadController.activeContainer() == null ? "minecraft"
                : loadController.activeContainer().getModId();
    }

    @Override
    public @NotNull ResourceLocation gtnhlib$getDefaultModernId() {
        String path = ModernIds.cleanId(getName().replaceAll("^potion\\.", ""));

        return new ResourceLocation(gtnhlib$owningMod.toLowerCase(Locale.ROOT), path);
    }
}
