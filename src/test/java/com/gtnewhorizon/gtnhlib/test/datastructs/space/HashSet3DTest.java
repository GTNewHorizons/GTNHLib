package com.gtnewhorizon.gtnhlib.test.datastructs.space;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.space.HashSet3D;
import com.gtnewhorizon.gtnhlib.space.ImmutableXYZ;
import com.gtnewhorizon.gtnhlib.space.XYZAddressable;

class HashSet3DTest {

    private HashSet3D set;

    @BeforeEach
    void setUp() {
        set = new HashSet3D();
    }

    @Test
    void addAndContainsByCoordinates() {
        assertTrue(set.add(1, 2, 3));
        assertTrue(set.contains(1, 2, 3));
        assertFalse(set.contains(3, 2, 1));
    }

    @Test
    void addingSameCoordinatesTwiceReturnsFalse() {
        assertTrue(set.add(1, 2, 3));
        assertFalse(set.add(1, 2, 3));
        assertEquals(1, set.size());
    }

    @Test
    void addAndContainsByAddressable() {
        ImmutableXYZ pos = new ImmutableXYZ(3, 4, 5);
        assertTrue(set.add(pos));
        assertTrue(set.contains(pos));
        assertTrue(set.contains(3, 4, 5));
    }

    @Test
    void removeByCoordinates() {
        set.add(5, 6, 7);
        assertTrue(set.remove(5, 6, 7));
        assertFalse(set.contains(5, 6, 7));
        assertFalse(set.remove(5, 6, 7));
    }

    @Test
    void removeByAddressable() {
        ImmutableXYZ pos = new ImmutableXYZ(7, 8, 9);
        set.add(pos);
        assertTrue(set.remove(pos));
        assertFalse(set.contains(pos));
    }

    @Test
    void forEachVisitsAllCoordinates() {
        set.add(1, 2, 3);
        set.add(4, 5, 6);

        Set<String> seen = new HashSet<>();
        set.forEach((x, y, z) -> seen.add(x + "," + y + "," + z));

        assertEquals(new HashSet<>(Arrays.asList("1,2,3", "4,5,6")), seen);
    }

    @Test
    void fastIteratorReusesSameMutableInstance() {
        set.add(1, 2, 3);
        set.add(4, 5, 6);

        var iter = set.fastIterator();
        XYZAddressable first = iter.next();
        assertTrue(iter.hasNext());
        XYZAddressable second = iter.next();

        assertSame(first, second);
        assertFalse(iter.hasNext());
    }

    @Test
    void slowIteratorReturnsIndependentInstances() {
        set.add(1, 2, 3);
        set.add(4, 5, 6);

        var iter = set.slowIterator();
        XYZAddressable first = iter.next();
        assertTrue(iter.hasNext());
        XYZAddressable second = iter.next();

        assertNotSame(first, second);
        assertFalse(iter.hasNext());

        Set<String> seen = new HashSet<>();
        seen.add(first.getX() + "," + first.getY() + "," + first.getZ());
        seen.add(second.getX() + "," + second.getY() + "," + second.getZ());
        assertEquals(new HashSet<>(Arrays.asList("1,2,3", "4,5,6")), seen);
    }

    @Test
    void slowStreamCountMatchesSize() {
        set.add(1, 2, 3);
        set.add(4, 5, 6);

        assertEquals(2, set.slowStream().count());
    }

    @Test
    void slowStreamElementsSurviveBuffering() {
        set.add(1, 2, 3);
        set.add(4, 5, 6);

        // sorted() buffers every element before emitting; slowStream must not share mutable state
        // across elements the way fastEntryStream() used to, or every entry would collapse to the last one.
        Set<String> collected = set.slowStream().sorted(Comparator.comparingInt(XYZAddressable::getX))
                .map(e -> e.getX() + "," + e.getY() + "," + e.getZ()).collect(Collectors.toCollection(HashSet::new));

        assertEquals(new HashSet<>(Arrays.asList("1,2,3", "4,5,6")), collected);
    }

    @Test
    void negativeCoordinatesAreDistinctFromPositive() {
        set.add(-1, -2, -3);
        set.add(1, 2, 3);

        assertTrue(set.contains(-1, -2, -3));
        assertTrue(set.contains(1, 2, 3));
        assertEquals(2, set.size());
    }
}
