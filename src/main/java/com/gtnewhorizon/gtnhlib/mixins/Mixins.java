package com.gtnewhorizon.gtnhlib.mixins;

import static com.gtnewhorizon.gtnhlib.GTNHLibConfig.autoTextureLoading;

import com.gtnewhorizon.gtnhlib.util.EarlyConfig;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Mixins implements IMixins {

    THREAD_SAFE_TESSELLATOR(Side.CLIENT, "MixinTessellator"),
    WAVEFRONT_VBO(Side.CLIENT, "MixinWavefrontObject"),
    AUTO_CONFIG_GUI(Side.CLIENT, "fml.MixinGuiModList"),
    EVENT_BUS_ACCESSOR(Side.COMMON, "fml.EventBusAccessor", "fml.EnumHolderAccessor"),
    TOOLTIP_RENDER(Side.CLIENT, "MixinGuiScreen"),
    EQUIPMENT_CHANGE_EVENT(Side.COMMON, "MixinEntityLivingBase"),
    BACKPORT_SERVER_TICKING(Side.COMMON, "MixinMinecraftServer"),
    GAME_RULES_API(Side.COMMON, "MixinGameRules"),
    DEBUG_TEXTURES(new MixinBuilder("Dump textures sizes")
            .addClientMixins("debug.MixinDynamicTexture", "debug.MixinTextureAtlasSprite").setPhase(Phase.EARLY)
            .setApplyIf(() -> Boolean.parseBoolean(System.getProperty("gtnhlib.debugtextures", "false")))),
    FONT_RENDERER(new MixinBuilder("Font rendering replacements").addClientMixins("MixinFontRenderer")
            .setPhase(Phase.EARLY).setApplyIf(() -> EarlyConfig.enableFontRendererMixin)),
    MODEL_TEXTURE_LOADING(new MixinBuilder("Automatically load model textures").addClientMixins(
            "models.FRMAccessor",
            "models.MixinFileResourcePack",
            "models.MixinFolderResourcePack",
            "models.SRRMAccessor").setPhase(Phase.EARLY).setApplyIf(() -> autoTextureLoading));

    private final MixinBuilder builder;

    Mixins(Side side, String... mixins) {
        builder = new MixinBuilder().addSidedMixins(side, mixins).setPhase(Phase.EARLY);
    }
}
