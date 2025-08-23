package com.gtnewhorizon.gtnhlib.client.model;

import org.joml.Matrix4f;

public interface BakeData {
    BakeData IDENTITY = new BakeData() {
        private final Matrix4f I = new Matrix4f();

        @Override
        public Matrix4f getAffineMatrix() {
            return I;
        }
    };

    /**
     * @return The affine matrix of the transformation. It may be reused - copy before mutating it!
     */
    Matrix4f getAffineMatrix();

    /**
     * @return True if textures shouldn't be rotated with the rest of the model; false otherwise.
     */
    default boolean lockUV() {
        return false;
    }
}
