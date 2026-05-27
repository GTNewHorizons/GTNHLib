package com.gtnewhorizon.gtnhlib.client.renderer;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Non-thread safe implementation of a zero-allocation Vertex transformer. Uses the matrix returned by
 * getIdentityMatrix() to transform the vertices.
 */
public final class VertexTransformer {

    public static final Matrix4f scratchMatrix = new Matrix4f();
    private static final Vector4f scratchVector = new Vector4f();

    public static Matrix4f resetIdentity() {
        return scratchMatrix.identity();
    }

    public static Vector4f getScratchVector() {
        return scratchVector;
    }

    public static void transformVertex(float x, float y, float z) {
        scratchVector.x = x;
        scratchVector.y = y;
        scratchVector.z = z;
        scratchVector.w = 1;

        MatrixHelper.transformVertex(scratchMatrix, scratchVector);
    }

    public static void addVertex(Tessellator tess, float x, float y, float z) {
        transformVertex(x, y, z);
        tess.addVertex(scratchVector.x, scratchVector.y, scratchVector.z);
    }
}
