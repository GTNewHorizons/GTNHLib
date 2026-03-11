package com.gtnewhorizon.gtnhlib.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.StdLCG;

public class StdLCGTest {

    /// Ensures {@link StdLCG} matches {@link Random}'s output in a variety of scenarios.
    @Test
    void testStdlibConformance() {
        // ...after construction,
        final var stdlib = new Random(900);
        final var lcg = new StdLCG(900);
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());

        // ...after post-construction seeding,
        stdlib.setSeed(9425125);
        lcg.setSeed(9425125);
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());

        // ...and using the non-stdlib seeding method.
        stdlib.setSeed(8502258);
        lcg.setSeedLCG(8502258);
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());
        assertEquals(stdlib.nextLong(), lcg.nextLong());
    }
}
