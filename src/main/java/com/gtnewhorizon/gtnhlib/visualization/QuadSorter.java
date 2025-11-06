package com.gtnewhorizon.gtnhlib.visualization;

import java.nio.FloatBuffer;
import java.util.BitSet;

import org.joml.Math;

import com.google.common.primitives.Floats;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrays;

public class QuadSorter {

    public static void sortStandardFormat(VertexFormat format, FloatBuffer buffer, int bufferLen, float x, float y, float z) {
        // Quad stride by Float size
        int quadStride = format.getVertexSize();

        int quadCount = bufferLen/quadStride/4;

        float[] distanceArray = new float[quadCount];
        int[] indicesArray = new int[quadCount];

        int vertexSizeInteger = quadStride / 4;

        for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
            distanceArray[quadIdx] = getDistanceSqSFP(buffer, x, y, z, vertexSizeInteger, quadIdx * quadStride);
            indicesArray[quadIdx] = quadIdx;
        }

        IntArrays.mergeSort(indicesArray, (a, b) -> Floats.compare(distanceArray[b], distanceArray[a]));

        rearrangeQuads(buffer, indicesArray, quadStride);
    }

    private static void rearrangeQuads(FloatBuffer buffer, int[] indicesArray, int stride) {
        BitSet bits = new BitSet();

        float[] temp1 = new float[stride];
        float[] temp2 = new float[stride];
        float[] temp3 = new float[stride];

        for (int l = bits.nextClearBit(0); l < indicesArray.length; l = bits.nextClearBit(l + 1)) {
            int m = indicesArray[l];

            if (m != l) {
                buffer.position(m * stride);
                buffer.get(temp1);

                int n = m;

                for (int o = indicesArray[m]; n != l; o = indicesArray[o]) {
                    buffer.position(n * stride);
                    buffer.get(temp2);
                    buffer.position(o * stride);
                    buffer.get(temp3);

                    buffer.position(n * stride);
                    buffer.put(temp3);
                    buffer.position(o * stride);
                    buffer.put(temp2);

                    bits.set(n);
                    n = o;
                }

                buffer.position(l * stride);
                buffer.put(temp1);
            }

            bits.set(l);
        }
    }

    private static float getDistanceSqSFP(FloatBuffer buffer, float xCenter, float yCenter, float zCenter, int stride, int start) {
        int vertexBase = start;
        final float x1 = buffer.get(vertexBase);
        final float y1 = buffer.get(vertexBase + 1);
        final float z1 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        final float x2 = buffer.get(vertexBase);
        final float y2 = buffer.get(vertexBase + 1);
        final float z2 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        final float x3 = buffer.get(vertexBase);
        final float y3 = buffer.get(vertexBase + 1);
        final float z3 = buffer.get(vertexBase + 2);

//        vertexBase += stride;
//        final float x4 = buffer.get(vertexBase);
//        final float y4 = buffer.get(vertexBase + 1);
//        final float z4 = buffer.get(vertexBase + 2);

        final float xa = x2 - x1;
        final float ya = y2 - y1;
        final float za = z2 - z1;

        final float xb = x3 - x1;
        final float yb = y3 - y1;
        final float zb = z3 - z1;

        float nx = org.joml.Math.fma(ya, zb, -za * yb);
        float ny = org.joml.Math.fma(za, xb, -xa * zb);
        float nz = Math.fma(xa, yb, -ya * xb);

        float mag = 1f / Math.sqrt(nx * nx + ny * ny + nz * nz);

        nx *= mag;
        ny *= mag;
        nz *= mag;

        return nx * xCenter + ny * yCenter + nz * zCenter;

//        final float xDist = ((x1 + x2 + x3 + x4) * 0.25F) - xCenter;
//        final float yDist = ((y1 + y2 + y3 + y4) * 0.25F) - yCenter;
//        final float zDist = ((z1 + z2 + z3 + z4) * 0.25F) - zCenter;
//
//        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }
}
