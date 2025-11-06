package com.gtnewhorizon.gtnhlib.client.model;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

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
}
