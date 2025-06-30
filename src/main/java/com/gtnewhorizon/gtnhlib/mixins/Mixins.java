package com.gtnewhorizon.gtnhlib.mixins;

import com.gtnewhorizon.gtnhlib.mixin.IMixins;
import com.gtnewhorizon.gtnhlib.mixin.MixinBuilder;

import lombok.Getter;

@Getter
public enum Mixins implements IMixins {

    // spotless:off
    TESSELLATOR(new MixinBuilder("Thread safety checks for the Tesselator")
        .addClientMixins("MixinTessellator")),
    WAVEFRONT_VBO(new MixinBuilder()
        .addClientMixins("MixinWavefrontObject")),
    GUI_MOD_LIST(new MixinBuilder("Auto config ui")
        .addClientMixins("fml.MixinGuiModList")),
    EVENT_BUS_ACCESSOR(new MixinBuilder()
        .addCommonMixins("fml.EventBusAccessor", "fml.EnumHolderAccessor")),
    TOOLTIP_RENDER(new MixinBuilder()
        .addClientMixins("MixinGuiScreen")),
    EQUIPMENT_CHANGE_EVENT(new MixinBuilder("Add equipment change Forge events")
        .addCommonMixins("MixinEntityLivingBase")),
    DEBUG_TEXTURES(new MixinBuilder("Dump textures sizes")
        .setApplyIf(() -> Boolean.parseBoolean(System.getProperty("gtnhlib.debugtextures", "false")))
        .addClientMixins("debug.MixinDynamicTexture", "debug.MixinTextureAtlasSprite")),
    SERVER_TICKING(new MixinBuilder("Backport MinecraftServer ticking methods")
        .addCommonMixins("MixinMinecraftServer"));
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder.setPhase(Phase.EARLY);
    }
}
