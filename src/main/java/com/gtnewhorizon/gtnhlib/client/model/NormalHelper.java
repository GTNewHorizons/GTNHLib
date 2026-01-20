package com.gtnewhorizon.gtnhlib.client.model;

import net.minecraft.client.renderer.Tessellator;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
     * Sets the normal based on a given normal matrix
     *
     * @param normal       The normal vector
     * @param dest         The vector that gets transformed
     * @param normalMatrix The normal matrix (typically the transpose of the inverse transformation matrix)
     */
    public static Vector3f setNormalTransformed(Vector3f normal, Vector3f dest, Matrix3f normalMatrix) {
        normalMatrix.transform(normal, dest).normalize();
        return dest;
    }

    /**
     * Same as the method above, but this one will mutate the passed Vector3f
     */
    public static Vector3f setNormalTransformed(Vector3f normal, Matrix3f normalMatrix) {
        return setNormalTransformed(normal, normal, normalMatrix);
    }

    public static void setNormalTransformed(Tessellator tessellator, Vector3f normal, Matrix3f normalMatrix) {
        setNormalTransformed(normal, normalMatrix);
        tessellator.setNormal(normal.x, normal.y, normal.z);
    }
}
