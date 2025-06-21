package com.gtnewhorizon.gtnhlib.mixins;

import java.util.List;
import java.util.function.Supplier;

import com.gtnewhorizon.gtnhlib.mixin.*;

import lombok.Getter;

@Getter
public enum Mixins implements IMixins {

    TESSELLATOR(new MixinBuilder("Thread safety checks for the Tesselator").addTargetedMod(TargetedMod.VANILLA)
            .setSide(Side.CLIENT).setPhase(Phase.EARLY).setApplyIf(() -> true).addMixinClasses("MixinTessellator")),
    WAVEFRONT_VBO(new MixinBuilder("WavefrontObject").addTargetedMod(TargetedMod.VANILLA).setSide(Side.CLIENT)
            .setPhase(Phase.EARLY).setApplyIf(() -> true).addMixinClasses("MixinWavefrontObject")),
    GUI_MOD_LIST(new MixinBuilder("Auto config ui").addTargetedMod(TargetedMod.VANILLA).setSide(Side.CLIENT)
            .setPhase(Phase.EARLY).addMixinClasses("fml.MixinGuiModList")),
    EVENT_BUS_ACCESSOR(new MixinBuilder("EventBusAccessor").addTargetedMod(TargetedMod.VANILLA).setSide(Side.BOTH)
            .setPhase(Phase.EARLY).addMixinClasses("fml.EventBusAccessor", "fml.EnumHolderAccessor")),
    TOOLTIP_RENDER(new MixinBuilder("TooltipRenderer").addMixinClasses("MixinGuiScreen")
            .addTargetedMod(TargetedMod.VANILLA).setApplyIf(() -> true).setPhase(Phase.EARLY).setSide(Side.CLIENT)),
    EQUIPMENT_CHANGE_EVENT(new MixinBuilder("Add equipment change Forge events").addTargetedMod(TargetedMod.VANILLA)
            .setSide(Side.BOTH).setPhase(Phase.EARLY).addMixinClasses("MixinEntityLivingBase").setApplyIf(() -> true)),
    DEBUG_TEXTURES(new MixinBuilder("Dump textures sizes").addTargetedMod(TargetedMod.VANILLA).setSide(Side.CLIENT)
            .setPhase(Phase.EARLY)
            .setApplyIf(() -> Boolean.parseBoolean(System.getProperty("gtnhlib.debugtextures", "false")))
            .addMixinClasses("debug.MixinDynamicTexture", "debug.MixinTextureAtlasSprite")),
    SERVER_TICKING(new MixinBuilder("Backport MinecraftServer ticking methods").addTargetedMod(TargetedMod.VANILLA)
            .setSide(Side.BOTH).setPhase(Phase.EARLY).addMixinClasses("MixinMinecraftServer").setApplyIf(() -> true)),
    BRIGADIER(new MixinBuilder("Brigadier").addTargetedMod(TargetedMod.VANILLA).setSide(Side.BOTH).setPhase(Phase.EARLY)
            .addMixinClasses("MixinCommandHandler", "MixinCommandHelp").setApplyIf(() -> true));

    private final List<String> mixinClasses;
    private final Supplier<Boolean> applyIf;
    private final Phase phase;
    private final Side side;
    private final List<ITargetedMod> targetedMods;
    private final List<ITargetedMod> excludedMods;

    Mixins(MixinBuilder builder) {
        this.mixinClasses = builder.mixinClasses;
        this.applyIf = builder.applyIf;
        this.side = builder.side;
        this.targetedMods = builder.targetedMods;
        this.excludedMods = builder.excludedMods;
        this.phase = builder.phase;
        if (this.targetedMods.isEmpty()) {
            throw new RuntimeException("No targeted mods specified for " + this.name());
        }
        if (this.applyIf == null) {
            throw new RuntimeException("No ApplyIf function specified for " + this.name());
        }
    }

}
