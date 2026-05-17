package com.gtnewhorizon.gtnhlib.client.model.wavefront;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;

public class CCBakedModel {

    public final List<Vector3f> vertices = new ArrayList<>();
    public final List<Vector2f> texCoords = new ArrayList<>();
    public final List<Vector3f> normals = new ArrayList<>();

    private int drawMode = GL11.GL_QUADS;

    private boolean hasColor;
    private int color;

    public CCBakedModel(boolean isQuads) {
        setDrawMode(isQuads);
    }

    public CCBakedModel setColor(int color) {
        hasColor = true;
        this.color = color;
        return this;
    }

    public CCBakedModel setDrawMode(int drawMode) {
        this.drawMode = drawMode;
        return this;
    }

    public CCBakedModel setDrawMode(boolean isQuads) {
        this.drawMode = isQuads ? GL11.GL_QUADS : GL11.GL_TRIANGLES;
        return this;
    }

    public IVertexArrayObject uploadToVBO() {
        final DirectTessellator tessellator = DirectTessellator.startCapturing();
        tessellator.startDrawing(drawMode);

        if (hasColor) {
            tessellator.setColorOpaque_I(color);
        }

        tessellate(tessellator);

        return DirectTessellator.stopCapturingToVBO(VertexBufferType.IMMUTABLE);
    }

    public void tessellate(Tessellator tessellator) {
        final boolean hasNormals = hasNormals();
        final boolean hasTexCoords = hasTexCoords();
        for (int i = 0; i < vertices.size(); i++) {
            final Vector3f vertex = vertices.get(i);

            if (hasNormals) {
                final Vector3f normal = normals.get(i);
                tessellator.setNormal(normal.x, normal.y, normal.z);
            }

            if (hasTexCoords) {
                final Vector2f uv = texCoords.get(i);
                tessellator.addVertexWithUV(vertex.x, vertex.y, vertex.z, uv.x, uv.y);
            } else {
                tessellator.addVertex(vertex.x, vertex.y, vertex.z);
            }
        }
    }

    public final boolean hasTexCoords() {
        return !texCoords.isEmpty();
    }

    public final boolean hasNormals() {
        return !normals.isEmpty();
    }
}
