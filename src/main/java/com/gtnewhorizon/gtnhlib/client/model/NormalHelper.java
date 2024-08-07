package com.gtnewhorizon.gtnhlib.client.model;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.client.renderer.util.DirectionUtil;

public class NormalHelper {

    /**
     * Computes the face normal of the given quad and saves it in the provided non-null vector. If
     * {@link NdQuadBuilder#nominalFace()} is set will optimize by confirming quad is parallel to that face and, if so,
     * use the standard normal for that face direction.
     *
     * <p>
     * Will work with triangles also. Assumes counter-clockwise winding order, which is the norm. Expects convex quads
     * with all points co-planar.
     */
    public static void computeFaceNormal(@NotNull Vector3f saveTo, NdQuadBuilder q) {
        final ForgeDirection nominalFace = q.nominalFace();

        if (nominalFace != ForgeDirection.UNKNOWN && GeometryHelper.isQuadParallelToFace(nominalFace, q)) {
            Vector3i vec = DirectionUtil.STEP[nominalFace.ordinal()];
            saveTo.set(vec.x, vec.y, vec.z);
            return;
        }

        final float x0 = q.getX(0);
        final float y0 = q.getY(0);
        final float z0 = q.getZ(0);
        final float x1 = q.getX(1);
        final float y1 = q.getY(1);
        final float z1 = q.getZ(1);
        final float x2 = q.getX(2);
        final float y2 = q.getY(2);
        final float z2 = q.getZ(2);
        final float x3 = q.getX(3);
        final float y3 = q.getY(3);
        final float z3 = q.getZ(3);

        final float dx0 = x2 - x0;
        final float dy0 = y2 - y0;
        final float dz0 = z2 - z0;
        final float dx1 = x3 - x1;
        final float dy1 = y3 - y1;
        final float dz1 = z3 - z1;

        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;

        float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

        if (l != 0) {
            normX /= l;
            normY /= l;
            normZ /= l;
        }

        saveTo.set(normX, normY, normZ);
    }
}
