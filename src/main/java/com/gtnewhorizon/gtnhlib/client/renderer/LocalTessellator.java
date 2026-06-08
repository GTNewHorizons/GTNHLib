package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.util.ModelQuadUtil.VERTEX_SIZE;

import java.nio.ByteBuffer;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.stacks.Vector3dStack;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

/**
 * Standalone thread-local Tesellator
 */
public class LocalTessellator extends Tessellator {

    boolean active = false;
    private final Vector3dStack storedTranslation = new Vector3dStack();

    public void storeTranslation() {
        storedTranslation.push();
        storedTranslation.set(xOffset, yOffset, zOffset);
    }

    public void restoreTranslation() {
        xOffset = storedTranslation.x;
        yOffset = storedTranslation.y;
        zOffset = storedTranslation.z;
        storedTranslation.pop();
    }

    public void discard() {
        isDrawing = false;
        reset();
    }

    public final void collectQuads(List<ModelQuadViewMutable> quads) {
        final int verticesPerPrimitive = drawMode == GL11.GL_QUADS ? 4 : 3;
        final int quadCount = vertexCount / verticesPerPrimitive;

        if (quadCount == 0) return;

        for (int quadI = 0; quadI < quadCount; quadI++) {
            final int srcOffset = quadI * (verticesPerPrimitive * VERTEX_SIZE);

            final ModelQuad quad = new ModelQuad();
            quad.setState(rawBuffer, srcOffset, drawMode, this.hasColor, this.hasNormals, this.hasBrightness);
            quads.add(quad);
        }
        this.discard();
    }

    public final void writeQuads(VertexFormat format, ByteBuffer out) {
        if (drawMode == GL11.GL_QUADS) {
            format.writeToBuffer(out, this.rawBuffer, this.rawBufferIndex);
        } else {
            throw new UnsupportedOperationException(
                    "Cannot call writeQuads when drawMode isn't GL_QUADS. This needs to be implemented!");
        }
        this.discard();
    }

    public final long writeToBuffer0(long writePtr, VertexFormat format) {
        writePtr = format.writeToBuffer0(writePtr, this.rawBuffer, this.rawBufferIndex);
        this.discard();
        return writePtr;
    }

    public final int getDataSize(VertexFormat format) {
        return vertexCount * format.getVertexSize();
    }

    public final int getDataSize(int vertexSize) {
        return vertexCount * vertexSize;
    }

    public final void exitLocalMode() {
        this.active = false;
        this.discard();
    }
}
