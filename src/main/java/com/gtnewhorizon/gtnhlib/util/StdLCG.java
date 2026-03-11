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

    /// Don't ask me why stdlib scrambles the seed, because I don't know.
    public StdLCG(long seed) {
        // this default constructor performs an atomic op on a global variable
        super(0);
        this.seed = scramble(seed);
    }

    public StdLCG() {
        this(System.nanoTime());
    }

    public static long scramble(long seed) {
        return (seed ^ multiplier) & mask;
    }

    /// Unlike {@link #setSeed(long)}, this doesn't reset {@link Random#haveNextNextGaussian}, because that field is
    /// private. Use this method if you don't care about the {@link #nextGaussian()} methods and want to avoid
    /// synchronizing on seed setting.
    public StdLCG setSeedLCG(long seed) {
        this.seed = scramble(seed);
        return this;
    }

    @Override
    protected int next(int bits) {
        seed = (seed * multiplier + increment) & mask;
        return (int) (seed >>> 48 - bits);
    }
}
