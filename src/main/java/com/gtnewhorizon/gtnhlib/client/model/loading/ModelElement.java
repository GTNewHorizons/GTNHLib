package com.gtnewhorizon.gtnhlib.client.model.loading;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis.X;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis;

@Desugar
public record ModelElement(Vector3f from, Vector3f to, @Nullable Rotation rotation, boolean shade, List<Face> faces) {

    @Desugar
    public record Face(ForgeDirection name, Vector4f uv, String texture, ForgeDirection cullFace, int rotation,
            int tintIndex) {}

    @Desugar
    public record Rotation(Vector3f origin, Axis axis, float angle, boolean rescale) {

        public static final Rotation NOOP = new Rotation(new Vector3f(0, 0, 0), X, 0, false);

        public Rotation(Vector3f origin, Axis axis, float angle, boolean rescale) {
            this.origin = origin;
            this.axis = axis;
            this.angle = (float) Math.toRadians(angle);
            this.rescale = rescale;
        }

        public Matrix4f getAffineMatrix() {

            // Subtract origin
            final Matrix4f ret = new Matrix4f().translation(-this.origin.x, -this.origin.y, -this.origin.z);

            // Rotate
            switch (this.axis) {
                case X -> ret.rotateLocalX(angle);
                case Y -> ret.rotateLocalY(angle);
                case Z -> ret.rotateLocalZ(angle);
            }

            // Add the origin back in
            return ret.translateLocal(this.origin.x, this.origin.y, this.origin.z);
        }
    }
}
