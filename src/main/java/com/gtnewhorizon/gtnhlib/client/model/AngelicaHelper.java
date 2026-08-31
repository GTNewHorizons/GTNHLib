package com.gtnewhorizon.gtnhlib.client.model;

import net.minecraft.client.renderer.Tessellator;

import com.gtnewhorizons.angelica.rendering.StateAwareTessellator;

class AngelicaHelper {

    /// This method doesn't need to mess with [com.gtnewhorizons.angelica.api.ExtCeleritasRenderBlocks] because that
    /// only affects [net.minecraft.client.renderer.RenderBlocks] methods, which we don't use.
    static void initAngelicaLighting(Tessellator tesselator) {
        if (!(tesselator instanceof StateAwareTessellator saTess)) return;

        saTess.angelica$setAppliedAo(true);
    }

    static void quadAngelicaLighting(Tessellator tesselator, boolean noDirectionalShading) {
        if (!(tesselator instanceof StateAwareTessellator saTess)) return;

        saTess.angelica$setNoDirectionalShading(noDirectionalShading);
    }

    static void resetAngelicaLighting(Tessellator tesselator) {
        if (!(tesselator instanceof StateAwareTessellator saTess)) return;

        saTess.angelica$setAppliedAo(false);
        saTess.angelica$setNoDirectionalShading(false);
    }
}
