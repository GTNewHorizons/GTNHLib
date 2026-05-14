package com.gtnewhorizon.gtnhlib.client.renderer.tessellator;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.gtnewhorizon.gtnhlib.client.renderer.CallbackTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.MatrixHelper;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorCallback;

public abstract class VertexTransformCallback implements TessellatorCallback {

    protected final Vector4f vec4 = new Vector4f();
    protected final Vector3f normalVec = new Vector3f();

    protected abstract Matrix4f getMVPMatrix();

    protected abstract Matrix4f getTextureMatrix();

    protected abstract Matrix4f getColorMatrix();

    protected abstract Matrix3f getNormalMatrix();

    @Override
    public void onVertex(CallbackTessellator tessellator, double x, double y, double z) {
        final Matrix4f mvpMatrix = getMVPMatrix();
        if (mvpMatrix == null) {
            TessellatorCallback.super.onVertex(tessellator, x, y, z);
            return;
        }
        vec4.x = (float) (x + tessellator.xOffset);
        vec4.y = (float) (y + tessellator.yOffset);
        vec4.z = (float) (z + tessellator.zOffset);
        vec4.w = 1;

        MatrixHelper.transformVertex(mvpMatrix, vec4);

        tessellator.writeVertex(vec4.x, vec4.y, vec4.z);
    }

    @Override
    public void onTextureUV(CallbackTessellator tessellator, double u, double v) {
        final Matrix4f textureMatrix = getTextureMatrix();
        if (textureMatrix == null) {
            TessellatorCallback.super.onTextureUV(tessellator, u, v);
            return;
        }
        vec4.x = (float) u;
        vec4.y = (float) v;
        vec4.z = 0;
        vec4.w = 1;

        MatrixHelper.transformUV(textureMatrix, vec4);

        tessellator.writeTextureUV(vec4.x, vec4.y);
    }

    @Override
    public void onColor(CallbackTessellator tessellator, int red, int green, int blue, int alpha) {
        final Matrix4f colorMatrix = getColorMatrix();
        if (colorMatrix == null) {
            TessellatorCallback.super.onColor(tessellator, red, green, blue, alpha);
            return;
        }
        vec4.x = red / 255f;
        vec4.y = green / 255f;
        vec4.z = blue / 255f;
        vec4.w = alpha / 255f;

        MatrixHelper.transformColor(colorMatrix, vec4);

        tessellator.writeColor(convertColor(vec4.x), convertColor(vec4.y), convertColor(vec4.z), convertColor(vec4.w));
    }

    private static int convertColor(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255f)));
    }

    @Override
    public void onNormal(CallbackTessellator tessellator, float nx, float ny, float nz) {
        final Matrix3f normalMatrix = getNormalMatrix();
        if (normalMatrix == null) {
            TessellatorCallback.super.onNormal(tessellator, nx, ny, nz);
            return;
        }

        normalVec.x = nx;
        normalVec.y = ny;
        normalVec.z = nz;

        MatrixHelper.transformNormal(normalMatrix, normalVec);

        tessellator.writeNormal(normalVec.x, normalVec.y, normalVec.z);
    }
}
