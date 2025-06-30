package com.gtnewhorizon.gtnhlib.mixins;

import com.gtnewhorizon.gtnhlib.mixin.IMixins;
import com.gtnewhorizon.gtnhlib.mixin.MixinBuilder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Mixins implements IMixins {

    // spotless:off
    TESSELLATOR(new MixinBuilder("Thread safety checks for the Tesselator")
        .setPhase(Phase.EARLY)
        .addClientMixins("MixinTessellator")),
    WAVEFRONT_VBO(new MixinBuilder()
        .setPhase(Phase.EARLY)
        .addClientMixins("MixinWavefrontObject")),
    GUI_MOD_LIST(new MixinBuilder("Auto config ui")
        .setPhase(Phase.EARLY)
        .addClientMixins("fml.MixinGuiModList")),
    EVENT_BUS_ACCESSOR(new MixinBuilder()
        .setPhase(Phase.EARLY)
        .addCommonMixins("fml.EventBusAccessor", "fml.EnumHolderAccessor")),
    TOOLTIP_RENDER(new MixinBuilder()
        .addClientMixins("MixinGuiScreen")
        .setPhase(Phase.EARLY)),
    EQUIPMENT_CHANGE_EVENT(new MixinBuilder("Add equipment change Forge events")
        .setPhase(Phase.EARLY)
        .addCommonMixins("MixinEntityLivingBase")),
    DEBUG_TEXTURES(new MixinBuilder("Dump textures sizes")
        .setPhase(Phase.EARLY)
        .setApplyIf(() -> Boolean.parseBoolean(System.getProperty("gtnhlib.debugtextures", "false")))
        .addClientMixins("debug.MixinDynamicTexture", "debug.MixinTextureAtlasSprite")),
    SERVER_TICKING(new MixinBuilder("Backport MinecraftServer ticking methods")
        .setPhase(Phase.EARLY)
        .addCommonMixins("MixinMinecraftServer"));
    // spotless:on

    private final MixinBuilder builder;
}
