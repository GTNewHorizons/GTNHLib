package com.gtnewhorizon.gtnhlib.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.StdLCG;

/// Ensures {@link StdLCG} matches {@link Random}'s output in a variety of scenarios.
public class StdLCGTest {

    @Test
    void testNextLongConformance() {
        // ...after construction,
        final var stdlib = new Random(900);
        final var lcg = new StdLCG(900);
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());

        // ...and after post-construction seeding
        stdlib.setSeed(9425125);
        lcg.setSeed(9425125);
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());
    }

    @Test
    void testNextGaussianConformance() {
        // ...after construction,
        final var stdlib = new Random(900);
        final var lcg = new StdLCG(900);
        assertEquals(stdlib.nextGaussian(), lcg.nextGaussian());
        assertEquals(stdlib.nextGaussian(), lcg.nextGaussian());
        assertEquals(stdlib.nextGaussian(), lcg.nextGaussian());

        // ...and after post-construction seeding
        stdlib.setSeed(9425125);
        lcg.setSeed(9425125);
        assertEquals(stdlib.nextGaussian(), lcg.nextGaussian());
        assertEquals(stdlib.nextGaussian(), lcg.nextGaussian());
        assertEquals(stdlib.nextGaussian(), lcg.nextGaussian());
    }
}
