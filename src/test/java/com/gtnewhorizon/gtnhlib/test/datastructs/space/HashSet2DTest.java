package com.gtnewhorizon.gtnhlib.test.datastructs.space;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.space.HashSet2D;
import com.gtnewhorizon.gtnhlib.space.ImmutableXZ;
import com.gtnewhorizon.gtnhlib.space.XZAddressable;

class HashSet2DTest {

    private HashSet2D set;

    @BeforeEach
    void setUp() {
        set = new HashSet2D();
    }

    @Test
    void addAndContainsByCoordinates() {
        assertTrue(set.add(1, 2));
        assertTrue(set.contains(1, 2));
        assertFalse(set.contains(2, 1));
    }

    @Test
    void addingSameCoordinatesTwiceReturnsFalse() {
        assertTrue(set.add(1, 2));
        assertFalse(set.add(1, 2));
        assertEquals(1, set.size());
    }

    @Test
    void addAndContainsByAddressable() {
        ImmutableXZ pos = new ImmutableXZ(3, 4);
        assertTrue(set.add(pos));
        assertTrue(set.contains(pos));
        assertTrue(set.contains(3, 4));
    }

    @Test
    void removeByCoordinates() {
        set.add(5, 6);
        assertTrue(set.remove(5, 6));
        assertFalse(set.contains(5, 6));
        assertFalse(set.remove(5, 6));
    }

    @Test
    void removeByAddressable() {
        ImmutableXZ pos = new ImmutableXZ(7, 8);
        set.add(pos);
        assertTrue(set.remove(pos));
        assertFalse(set.contains(pos));
    }

    @Test
    void forEachVisitsAllCoordinates() {
        set.add(1, 2);
        set.add(3, 4);

        Set<String> seen = new HashSet<>();
        set.forEach((x, z) -> seen.add(x + "," + z));

        assertEquals(new HashSet<>(Arrays.asList("1,2", "3,4")), seen);
    }

    @Test
    void fastIteratorReusesSameMutableInstance() {
        set.add(1, 2);
        set.add(3, 4);

        var iter = set.fastIterator();
        XZAddressable first = iter.next();
        assertTrue(iter.hasNext());
        XZAddressable second = iter.next();

        assertSame(first, second);
        assertFalse(iter.hasNext());
    }

    @Test
    void slowIteratorReturnsIndependentInstances() {
        set.add(1, 2);
        set.add(3, 4);

        var iter = set.slowIterator();
        XZAddressable first = iter.next();
        assertTrue(iter.hasNext());
        XZAddressable second = iter.next();

        assertNotSame(first, second);
        assertFalse(iter.hasNext());

        Set<String> seen = new HashSet<>();
        seen.add(first.getX() + "," + first.getZ());
        seen.add(second.getX() + "," + second.getZ());
        assertEquals(new HashSet<>(Arrays.asList("1,2", "3,4")), seen);
    }

    @Test
    void slowStreamCountMatchesSize() {
        set.add(1, 2);
        set.add(3, 4);

        assertEquals(2, set.slowStream().count());
    }

    @Test
    void slowStreamElementsSurviveBuffering() {
        set.add(1, 2);
        set.add(3, 4);

        // sorted() buffers every element before emitting; slowStream must not share mutable state
        // across elements the way fastEntryStream() used to, or every entry would collapse to the last one.
        Set<String> collected = set.slowStream().sorted(Comparator.comparingInt(XZAddressable::getX))
                .map(e -> e.getX() + "," + e.getZ()).collect(Collectors.toCollection(HashSet::new));

        assertEquals(new HashSet<>(Arrays.asList("1,2", "3,4")), collected);
    }

    @Test
    void negativeCoordinatesAreDistinctFromPositive() {
        set.add(-1, -2);
        set.add(1, 2);

        assertTrue(set.contains(-1, -2));
        assertTrue(set.contains(1, 2));
        assertEquals(2, set.size());
    }
}
