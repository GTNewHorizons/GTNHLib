package com.gtnewhorizon.gtnhlib.client.renderer.cel.util;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Z;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Z;
import static java.lang.Math.abs;
import static java.lang.Math.max;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.api.util.NormI8;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

public class QuadUtil {

    public static ModelQuadFacing findLightFace(float x, float y, float z) {
        var max = (max(max((abs(x)), abs(y)), abs(z)));

        if (max == x) return x < 0 ? NEG_X : POS_X;
        if (max == z) return z < 0 ? NEG_Z : POS_Z;
        return y < 0 ? NEG_Y : POS_Y;
    }

    public static ModelQuadFacing findNormalFace(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return ModelQuadFacing.UNASSIGNED;
        }

        float maxDot = 0;
        ModelQuadFacing closestFace = null;

        for (ModelQuadFacing face : ModelQuadFacing.DIRECTIONS) {
            float dot = (x * face.getStepX()) + (y * face.getStepY()) + (z * face.getStepZ());

            if (dot > maxDot) {
                maxDot = dot;
                closestFace = face;
            }
        }

        if (closestFace != null && Math.abs(maxDot - 1.0f) < 1.0E-5F) {
            return closestFace;
        }

        return ModelQuadFacing.UNASSIGNED;
    }

    public static ModelQuadFacing findNormalFace(int normal) {
        return findNormalFace(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
    }

}
