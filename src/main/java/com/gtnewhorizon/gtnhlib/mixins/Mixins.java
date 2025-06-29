package com.gtnewhorizon.gtnhlib.mixins;

import com.gtnewhorizon.gtnhlib.mixin.IMixins;
import com.gtnewhorizon.gtnhlib.mixin.MixinBuilder;
import com.gtnewhorizon.gtnhlib.mixin.Phase;
import com.gtnewhorizon.gtnhlib.mixin.TargetedMod;

public enum Mixins implements IMixins {

    // spotless:off
    TESSELLATOR(new MixinBuilder("Thread safety checks for the Tesselator")
        .addTargetedMod(TargetedMod.VANILLA)
        .setPhase(Phase.EARLY)
        .setApplyIf(() -> true)
        .addClientMixins("MixinTessellator")),
    WAVEFRONT_VBO(new MixinBuilder("WavefrontObject")
        .addTargetedMod(TargetedMod.VANILLA)
        .setPhase(Phase.EARLY)
        .setApplyIf(() -> true)
        .addClientMixins("MixinWavefrontObject")),
    GUI_MOD_LIST(new MixinBuilder("Auto config ui")
        .addTargetedMod(TargetedMod.VANILLA)
        .setPhase(Phase.EARLY)
        .addClientMixins("fml.MixinGuiModList")),
    EVENT_BUS_ACCESSOR(new MixinBuilder("EventBusAccessor")
        .addTargetedMod(TargetedMod.VANILLA)
        .setPhase(Phase.EARLY)
        .addCommonMixins("fml.EventBusAccessor", "fml.EnumHolderAccessor")),
    TOOLTIP_RENDER(new MixinBuilder("TooltipRenderer")
        .addClientMixins("MixinGuiScreen")
        .addTargetedMod(TargetedMod.VANILLA)
        .setApplyIf(() -> true)
        .setPhase(Phase.EARLY)),
    EQUIPMENT_CHANGE_EVENT(new MixinBuilder("Add equipment change Forge events")
        .addTargetedMod(TargetedMod.VANILLA)
        .setPhase(Phase.EARLY)
        .addCommonMixins("MixinEntityLivingBase")
        .setApplyIf(() -> true)),
    DEBUG_TEXTURES(new MixinBuilder("Dump textures sizes")
        .addTargetedMod(TargetedMod.VANILLA)
        .setPhase(Phase.EARLY)
        .setApplyIf(() -> Boolean.parseBoolean(System.getProperty("gtnhlib.debugtextures", "false")))
        .addClientMixins("debug.MixinDynamicTexture", "debug.MixinTextureAtlasSprite")),
    SERVER_TICKING(new MixinBuilder("Backport MinecraftServer ticking methods")
        .addTargetedMod(TargetedMod.VANILLA)
        .setPhase(Phase.EARLY)
        .addCommonMixins("MixinMinecraftServer")
        .setApplyIf(() -> true));
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
