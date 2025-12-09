package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.WavefrontObject;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IModelCustomExt;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;

// TODO Sisyphus: get rid of renderAllVBO
@Mixin(value = WavefrontObject.class, remap = false)
public abstract class MixinWavefrontObject implements IModelCustomExt {

    @Unique
    private VertexBuffer vertexBuffer;

    @Shadow
    private GroupObject currentGroupObject;

    @Shadow
    public abstract void tessellateAll(Tessellator tessellator);

    @Override
    public void rebuildVBO() {
        rebuild(false);
    }

    private void rebuild(boolean vao) {
        if (currentGroupObject == null) {
            throw new RuntimeException("No group object selected");
        }
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
        }
        TessellatorManager.startCapturing();
        final CapturingTessellator tess = (CapturingTessellator) TessellatorManager.get();
        tess.startDrawing(currentGroupObject.glDrawingMode);
        tessellateAll(tess);

        this.vertexBuffer = vao ? TessellatorManager.stopCapturingToVAO(DefaultVertexFormat.POSITION_TEXTURE_NORMAL)
                : TessellatorManager.stopCapturingToVBO(DefaultVertexFormat.POSITION_TEXTURE_NORMAL);
    }

    @Override
    public void renderAllVBO() {
        if (vertexBuffer == null) {
            rebuild(false);
        }
        vertexBuffer.render();
    }

    @Override
    public void renderAllVAO() {
        if (vertexBuffer == null) {
            rebuild(true);
        }
        vertexBuffer.render();
    }
}
