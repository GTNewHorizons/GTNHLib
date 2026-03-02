package com.gtnewhorizon.gtnhlib.client.model;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class NormalHelper {

    /**
     * Computes a Normal matrix using a transformation matrix. <br>
     * The normal matrix is the transpose of the inversed model matrix. <br>
     * This is what OpenGL uses to properly transform the normals when there are non-uniform scalings present <br>
     * For more information, look up "OpenGL Normal matrix"
     */
    public static Matrix3f getNormalMatrix(Matrix4f transformationMatrix, Matrix3f dest) {
        return dest.set(transformationMatrix).invert().transpose();
    }

    /**
     * Same as the method above, but now creates a new Matrix3f object. Only use if the method gets called only once.
     */
    public static Matrix3f getNormalMatrix(Matrix4f transformationMatrix) {
        return new Matrix3f(transformationMatrix).invert().transpose();
    }

    /**
     * Sets the normal based on the given normal matrix
     *
     * @param normal       The normal vector
     * @param dest         The vector that gets transformed
     * @param normalMatrix The normal matrix (typically the transpose of the inverse transformation matrix)
     */
    public static Vector3f setNormalTransformed(Vector3fc normal, Vector3f dest, Matrix3f normalMatrix) {
        normalMatrix.transform(normal, dest).normalize();
        return dest;
    }

    /**
     * Sets the normal based on the given normal matrix and mutates the passed-in Vector3f
     */
    public static Vector3f setNormalTransformed(Vector3f normal, Matrix3f normalMatrix) {
        return setNormalTransformed(normal, normal, normalMatrix);
    }

    /**
     * Sets the normal of the Tessellator based on the given normal matrix and mutates the passed-in Vector3f
     */
    public static void setNormalTransformed(Tessellator tessellator, Vector3f normal, Matrix3f normalMatrix) {
        setNormalTransformed(normal, normalMatrix);
        tessellator.setNormal(normal.x, normal.y, normal.z);
    }

    public static void setNormalTransformed(Tessellator tessellator, Vector3fc normal, Vector3f dest,
            Matrix3f normalMatrix) {
        setNormalTransformed(normal, dest, normalMatrix);
        tessellator.setNormal(dest.x, dest.y, dest.z);
    }
}
