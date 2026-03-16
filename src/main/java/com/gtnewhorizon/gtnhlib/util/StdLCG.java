package com.gtnewhorizon.gtnhlib.util;

import java.util.Random;

/// A basic LCG, based on Knuth's work and using the same constants as {@link Random}. As such, it produces the same
/// output, just MUCH faster since it's not reliant on atomic ops. However, each instance can only be used by one thread
/// at a time.
public class StdLCG extends Random {

    protected static final long mask = -1L >>> 16;
    protected static final long multiplier = 0x5DEECE66DL;
    protected static final long increment = 11;
    protected long seed;
    protected boolean haveNextNextGaussian = false;
    protected double nextNextGaussian = 0.0;

    /// Don't ask me why stdlib scrambles the seed, because I don't know.
    public StdLCG(long seed) {
        // this default constructor performs an atomic op on a global variable
        super(seed);
    }

    public StdLCG() {
        this(System.nanoTime());
    }

    public static long scramble(long seed) {
        return (seed ^ multiplier) & mask;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = scramble(seed);
        haveNextNextGaussian = false;
    }

    @Override
    protected int next(int bits) {
        seed = (seed * multiplier + increment) & mask;
        return (int) (seed >>> 48 - bits);
    }

    /// A reimplementation of Knuth's algorithm, except I read the 2nd edition instead of the 3rd. It's probably close
    /// enough.
    @Override
    public double nextGaussian() {
        if (haveNextNextGaussian) {
            haveNextNextGaussian = false;
            return nextNextGaussian;
        }

        double s, v1, v2;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
            // Knuth doesn't say to return if s==0, but stdlib does. We need to match stdlib, so another clause it is.
        } while (s >= 1 || s == 0);

        final var tmpRoot = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextNextGaussian = v2 * tmpRoot;
        haveNextNextGaussian = true;
        return v1 * tmpRoot;
    }
}
