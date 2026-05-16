package com.gtnewhorizon.gtnhlib.client.renderer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.gtnewhorizon.gtnhlib.client.model.NormalHelper;

public final class MatrixHelper {

    private static final Matrix4f IDENTITY = new Matrix4f();

    public static boolean isIdentity(Matrix4f m) {
        return (m.properties() & Matrix4f.PROPERTY_IDENTITY) != 0 || m.equals(IDENTITY, 1e-6f);
    }

    public static void transformVertex(Matrix4f modelViewProjection, Vector4f dest) {
        modelViewProjection.transform(dest);

        if (dest.w != 1.0f) {
            dest.x /= dest.w;
            dest.y /= dest.w;
            dest.z /= dest.w;
        }
    }

    public static void transformUV(Matrix4f texture, Vector4f dest) {
        texture.transform(dest);

        if (dest.w != 1.0f) {
            dest.x /= dest.w;
            dest.y /= dest.w;
        }
    }

    public static void transformColor(Matrix4f color, Vector4f dest) {
        color.transform(dest);
    }

    public static Matrix3f getNormalMatrix(Matrix4f modelviewMatrix, Matrix3f dest) {
        return NormalHelper.getNormalMatrix(modelviewMatrix, dest);
    }

    public static void transformNormal(Matrix3f normalMat, Vector3f dest) {
        normalMat.transform(dest);
        dest.normalize();
    }
}
