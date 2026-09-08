/*****************************************************************************
 * J3D.org Copyright (c) 2000 Java Source This source is licensed under the GNU LGPL v2.1 Please read
 * http://www.gnu.org/copyleft/lgpl.html for more information This software comes with the standard NO WARRANTY
 * disclaimer for any purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package com.gtnewhorizon.gtnhlib.noise;

import java.util.Random;

/// A 2d or 3d perlin sampler.
/// This is largely copied from
/// [RWG](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/37046ef9a2f807ae4feb0ad87e7ae65f756839a6/src/main/java/rwg/util/PerlinNoise.java),
/// with the irrelevant code stripped out.
public class PerlinSampler implements NoiseSampler {

    // Constants for setting up the Perlin-1 noise functions
    private static final int B = 0x1000;
    private static final int BM = 0xff;
    private static final int N = 0x1000;

    /** Default sample size to work with */
    private static final int DEFAULT_SAMPLE_SIZE = 256;

    /** Permutation array for the improved noise function */
    private final int[] p_imp;

    /** P array for perline 1 noise */
    private final int[] p = new int[B + B + 2];

    private final float[] g3 = new float[(B + B + 2) * 3];
    private final float[] g2 = new float[(B + B + 2) * 2];

    /**
     * Create a new noise creator with the default seed value
     */
    public PerlinSampler() {
        this(100);
    }

    /**
     * Create a new noise creator with the given seed value for the randomness
     *
     * @param seed The seed value to use
     */
    public PerlinSampler(long seed) {
        p_imp = new int[DEFAULT_SAMPLE_SIZE << 1];

        int i, j, k;
        Random rand = new Random(seed);

        // Calculate the table of pseudo-random coefficients.
        for (i = 0; i < DEFAULT_SAMPLE_SIZE; i++) p_imp[i] = i;

        // generate the pseudo-random permutation table.
        while (--i > 0) {
            k = p_imp[i];
            j = (int) (rand.nextLong() & DEFAULT_SAMPLE_SIZE);
            p_imp[i] = p_imp[j];
            p_imp[j] = k;
        }

        initPerlin1(rand);
    }

    /**
     * Computes noise function for three dimensions at the point (x,y,z).
     *
     * @param x x dimension parameter
     * @param y y dimension parameter
     * @param z z dimension parameter
     * @return the noise value at the point (x, y, z)
     */
    @Override
    public double sample(double x, double y, double z) {
        // Constraint the point to a unit cube
        double fx = Math.floor(x);
        double fy = Math.floor(y);
        double fz = Math.floor(z);
        int uc_x = (int) fx & 255;
        int uc_y = (int) fy & 255;
        int uc_z = (int) fz & 255;

        // Relative location of the point in the unit cube
        double xo = x - fx;
        double yo = y - fy;
        double zo = z - fz;

        // Fade curves for x, y and z
        double u = fade(xo);
        double v = fade(yo);
        double w = fade(zo);

        // Generate a hash for each coordinate to find out where in the cube
        // it lies.
        int a = p_imp[uc_x] + uc_y;
        int aa = p_imp[a] + uc_z;
        int ab = p_imp[a + 1] + uc_z;

        int b = p_imp[uc_x + 1] + uc_y;
        int ba = p_imp[b] + uc_z;
        int bb = p_imp[b + 1] + uc_z;

        // blend results from the 8 corners based on the noise function
        double c1 = grad(p_imp[aa], xo, yo, zo);
        double c2 = grad(p_imp[ba], xo - 1, yo, zo);
        double c3 = grad(p_imp[ab], xo, yo - 1, zo);
        double c4 = grad(p_imp[bb], xo - 1, yo - 1, zo);
        double c5 = grad(p_imp[aa + 1], xo, yo, zo - 1);
        double c6 = grad(p_imp[ba + 1], xo - 1, yo, zo - 1);
        double c7 = grad(p_imp[ab + 1], xo, yo - 1, zo - 1);
        double c8 = grad(p_imp[bb + 1], xo - 1, yo - 1, zo - 1);

        return lerp(w, lerp(v, lerp(u, c1, c2), lerp(u, c3, c4)), lerp(v, lerp(u, c5, c6), lerp(u, c7, c8)));
    }

    /**
     * Create noise in a 2D space using the original Perlin noise algorithm.
     *
     * @param x The X coordinate of the location to sample
     * @param y The Y coordinate of the location to sample
     * @return A noisy value at the given position
     */
    @Override
    public double sample(double x, double y) {
        double t = x + N;
        t = Math.abs(t);
        int bx0 = (int) t & BM;
        int bx1 = bx0 + 1 & BM;
        double rx0 = t - (int) t;
        double rx1 = rx0 - 1;

        t = y + N;
        t = Math.abs(t);
        int by0 = (int) t & BM;
        int by1 = by0 + 1 & BM;
        double ry0 = t - (int) t;
        double ry1 = ry0 - 1;

        int i = p[bx0];
        int j = p[bx1];

        int b00 = p[i + by0];
        int b10 = p[j + by0];
        int b01 = p[i + by1];
        int b11 = p[j + by1];

        double sx = sCurve(rx0);
        double sy = sCurve(ry0);

        double u = rx0 * g2[b00 * 2] + ry0 * g2[b00 * 2 + 1];
        double v = rx1 * g2[b10 * 2] + ry0 * g2[b10 * 2 + 1];
        double a = lerp(sx, u, v);

        u = rx0 * g2[b01 * 2] + ry1 * g2[b01 * 2 + 1];
        v = rx1 * g2[b11 * 2] + ry1 * g2[b11 * 2 + 1];
        double b = lerp(sx, u, v);

        return lerp(sy, a, b);
    }

    /**
     * Simple lerp function using doubles.
     */
    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /**
     * Fade curve calculation which is 6t^5 - 15t^4 + 10t^3. This is the new algorithm, where the old one used to be
     * 3t^2 - 2t^3.
     *
     * @param t The t parameter to calculate the fade for
     * @return the drop-off amount.
     */
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /**
     * Calculate the gradient function based on the hash code.
     */
    private double grad(int hash, double x, double y, double z) {
        // Convert low 4 bits of hash code into 12 gradient directions.
        int h = hash & 15;
        double u = h < 8 || h == 12 || h == 13 ? x : y;
        double v = h < 4 || h == 12 || h == 13 ? y : z;

        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /**
     * S-curve function for value distribution for Perlin-1 noise function.
     */
    private double sCurve(double t) {
        return t * t * (3 - 2 * t);
    }

    /**
     * 2D-vector normalization function.
     */
    private void normalize2(float[] v, int off) {
        float s = (float) (1 / Math.sqrt(v[off] * v[off] + v[off + 1] * v[off + 1]));
        v[off] *= s;
        v[off + 1] *= s;
    }

    /**
     * 3D-vector normalization function.
     */
    private void normalize3(float[] v, int off) {
        float s = (float) (1 / Math.sqrt(v[off] * v[off] + v[off + 1] * v[off + 1] + v[off + 2] * v[off + 2]));
        v[off] *= s;
        v[off + 1] *= s;
        v[off + 2] *= s;
    }

    /**
     * Initialize the lookup arrays used by Perlin 1 function.
     */
    private void initPerlin1(Random rand) {
        int i, j, k;

        for (i = 0; i < B; i++) {
            p[i] = i;

            for (j = 0; j < 2; j++) g2[i * 2 + j] = (float) (rand.nextDouble() * Integer.MAX_VALUE % (B + B) - B) / B;
            normalize2(g2, i * 2);

            for (j = 0; j < 3; j++) g3[i * 3 + j] = (float) (rand.nextDouble() * Integer.MAX_VALUE % (B + B) - B) / B;
            normalize3(g3, i * 3);
        }

        while (--i > 0) {
            k = p[i];
            j = (int) (rand.nextDouble() * Integer.MAX_VALUE % B);
            p[i] = p[j];
            p[j] = k;
        }

        for (i = 0; i < B + 2; i++) {
            p[B + i] = p[i];
            for (j = 0; j < 2; j++) g2[(B + i) * 2 + j] = g2[i * 2 + j];
            for (j = 0; j < 3; j++) g3[(B + i) * 3 + j] = g3[i * 3 + j];
        }
    }
}
