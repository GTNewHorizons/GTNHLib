package com.gtnewhorizon.gtnhlib.test.datastructs.space;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.space.HashMap3D;
import com.gtnewhorizon.gtnhlib.datastructs.space.HashMap3D.Entry3D;
import com.gtnewhorizon.gtnhlib.space.ImmutableXYZ;

class HashMap3DTest {

    private HashMap3D<String> map;

    @BeforeEach
    void setUp() {
        map = new HashMap3D<>();
    }

    @Test
    void putAndGetByCoordinates() {
        assertNull(map.put(1, 2, 3, "A"));
        assertEquals("A", map.get(1, 2, 3));
        assertNull(map.get(3, 2, 1));
    }

    @Test
    void putAndGetByAddressable() {
        ImmutableXYZ pos = new ImmutableXYZ(3, 4, 5);
        map.put(pos, "B");
        assertEquals("B", map.get(pos));
        assertEquals("B", map.get(3, 4, 5));
    }

    @Test
    void containsKey() {
        assertFalse(map.containsKey(1, 1, 1));
        map.put(1, 1, 1, "A");
        assertTrue(map.containsKey(1, 1, 1));
        assertTrue(map.containsKey(new ImmutableXYZ(1, 1, 1)));
    }

    @Test
    void removeByCoordinatesReturnsOldValue() {
        map.put(5, 6, 7, "A");
        assertEquals("A", map.remove(5, 6, 7));
        assertNull(map.get(5, 6, 7));
        assertNull(map.remove(5, 6, 7));
    }

    @Test
    void removeByAddressable() {
        ImmutableXYZ pos = new ImmutableXYZ(7, 8, 9);
        map.put(pos, "A");
        assertEquals("A", map.remove(pos));
        assertFalse(map.containsKey(pos));
    }

    @Test
    void removeWithValueMatchOnlyRemovesOnMatch() {
        map.put(1, 1, 1, "A");
        assertFalse(map.remove(1, 1, 1, "B"));
        assertEquals("A", map.get(1, 1, 1));
        assertTrue(map.remove(1, 1, 1, "A"));
        assertNull(map.get(1, 1, 1));
    }

    @Test
    void computeIfAbsentComputesOnceAndReusesExisting() {
        assertEquals("A", map.computeIfAbsent(1, 1, 1, (x, y, z) -> "A"));
        assertEquals("A", map.computeIfAbsent(1, 1, 1, (x, y, z) -> "B"));
    }

    @Test
    void computeAppliesRemappingFunctionWithCoordinates() {
        map.put(1, 1, 1, "A");
        String result = map.compute(1, 1, 1, (x, y, z, v) -> v + x + y + z);
        assertEquals("A111", result);
        assertEquals("A111", map.get(1, 1, 1));
    }

    @Test
    void computeIfPresentSkipsMissingKeys() {
        assertNull(map.computeIfPresent(1, 1, 1, (x, y, z, v) -> "shouldNotRun"));
        map.put(1, 1, 1, "A");
        assertEquals("A!", map.computeIfPresent(1, 1, 1, (x, y, z, v) -> v + "!"));
    }

    @Test
    void getOrDefaultReturnsDefaultWhenMissing() {
        assertEquals("D", map.getOrDefault(1, 1, 1, "D"));
        map.put(1, 1, 1, "A");
        assertEquals("A", map.getOrDefault(1, 1, 1, "D"));
    }

    @Test
    void putIfAbsentTreatsNullValueAsPresent() {
        assertNull(map.putIfAbsent(1, 1, 1, null));
        assertTrue(map.containsKey(1, 1, 1));
        assertNull(map.putIfAbsent(1, 1, 1, "A"));
        assertNull(map.get(1, 1, 1));
    }

    @Test
    void replaceOnlyAffectsExistingKeys() {
        assertNull(map.replace(1, 1, 1, "A"));
        assertFalse(map.containsKey(1, 1, 1));
        map.put(1, 1, 1, "A");
        assertEquals("A", map.replace(1, 1, 1, "B"));
        assertEquals("B", map.get(1, 1, 1));
    }

    @Test
    void replaceWithOldValueMatchOnlyReplacesOnMatch() {
        map.put(1, 1, 1, "A");
        assertFalse(map.replace(1, 1, 1, "WRONG", "B"));
        assertEquals("A", map.get(1, 1, 1));
        assertTrue(map.replace(1, 1, 1, "A", "B"));
        assertEquals("B", map.get(1, 1, 1));
    }

    @Test
    void mergeCombinesValues() {
        map.put(1, 1, 1, "A");
        map.merge(1, 1, 1, "B", (oldV, newV) -> oldV + newV);
        assertEquals("AB", map.get(1, 1, 1));
    }

    @Test
    void forEachVisitsAllEntriesWithCoordinates() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        Set<String> seen = new HashSet<>();
        map.forEach((x, y, z, v) -> seen.add(x + "," + y + "," + z + "=" + v));

        assertEquals(new HashSet<>(Arrays.asList("1,2,3=A", "4,5,6=B")), seen);
    }

    @Test
    void replaceAllRewritesValuesBasedOnCoordinates() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        map.replaceAll((x, y, z, v) -> v + x + y + z);

        assertEquals("A123", map.get(1, 2, 3));
        assertEquals("B456", map.get(4, 5, 6));
    }

    @Test
    void fastEntryIterableExposesCoordinatesAndValues() {
        map.put(1, 2, 3, "A");

        var entries = map.fastEntryIterable().iterator();
        assertTrue(entries.hasNext());
        var entry = entries.next();
        assertEquals(1, entry.getX());
        assertEquals(2, entry.getY());
        assertEquals(3, entry.getZ());
        assertEquals("A", entry.getValue());
        assertFalse(entries.hasNext());
    }

    @Test
    void slowIteratorReturnsIndependentEntries() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        var iter = map.slowIterator();
        Entry3D<String> first = iter.next();
        Entry3D<String> second = iter.next();

        assertNotSame(first, second);
        Set<String> seen = new HashSet<>();
        seen.add(first.getX() + "," + first.getY() + "," + first.getZ() + "=" + first.getValue());
        seen.add(second.getX() + "," + second.getY() + "," + second.getZ() + "=" + second.getValue());
        assertEquals(new HashSet<>(Arrays.asList("1,2,3=A", "4,5,6=B")), seen);
    }

    @Test
    void slowStreamCountMatchesSize() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        assertEquals(2, map.slowStream().count());
    }

    @Test
    void slowStreamElementsSurviveBuffering() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        // sorted() buffers every element before emitting; slowStream must not share mutable state
        // across entries the way fastEntryStream() used to, or every entry would collapse to the last one.
        Set<String> collected = map.slowStream().sorted(Comparator.comparingInt(Entry3D::getX))
                .map(e -> e.getX() + "," + e.getY() + "," + e.getZ() + "=" + e.getValue())
                .collect(Collectors.toCollection(HashSet::new));

        assertEquals(new HashSet<>(Arrays.asList("1,2,3=A", "4,5,6=B")), collected);
    }

    @Test
    void negativeCoordinatesAreDistinctFromPositive() {
        map.put(-1, -2, -3, "Neg");
        map.put(1, 2, 3, "Pos");

        assertEquals("Neg", map.get(-1, -2, -3));
        assertEquals("Pos", map.get(1, 2, 3));
        assertEquals(2, map.size());
    }

    @Test
    void computeIfAbsentByAddressableDelegatesToCoordinates() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        assertEquals("A", map.computeIfAbsent(pos, (x, y, z) -> "A"));
        assertEquals("A", map.computeIfAbsent(pos, (x, y, z) -> "B"));
    }

    @Test
    void computeByAddressableDelegatesToCoordinates() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        map.put(pos, "A");
        assertEquals("A111", map.compute(pos, (x, y, z, v) -> v + x + y + z));
    }

    @Test
    void computeIfPresentByAddressableDelegatesToCoordinates() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        assertNull(map.computeIfPresent(pos, (x, y, z, v) -> "shouldNotRun"));
        map.put(pos, "A");
        assertEquals("A!", map.computeIfPresent(pos, (x, y, z, v) -> v + "!"));
    }

    @Test
    void getOrDefaultByAddressableDelegatesToCoordinates() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        assertEquals("D", map.getOrDefault(pos, "D"));
        map.put(pos, "A");
        assertEquals("A", map.getOrDefault(pos, "D"));
    }

    @Test
    void putIfAbsentByAddressableDelegatesToCoordinates() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        assertNull(map.putIfAbsent(pos, "A"));
        assertEquals("A", map.putIfAbsent(pos, "B"));
        assertEquals("A", map.get(pos));
    }

    @Test
    void removeWithValueMatchByAddressable() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        map.put(pos, "A");
        assertFalse(map.remove(pos, "B"));
        assertTrue(map.remove(pos, "A"));
        assertNull(map.get(pos));
    }

    @Test
    void replaceByAddressable() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        assertNull(map.replace(pos, "A"));
        map.put(pos, "A");
        assertEquals("A", map.replace(pos, "B"));
        assertEquals("B", map.get(pos));
    }

    @Test
    void replaceWithOldValueMatchByAddressable() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        map.put(pos, "A");
        assertFalse(map.replace(pos, "WRONG", "B"));
        assertTrue(map.replace(pos, "A", "B"));
        assertEquals("B", map.get(pos));
    }

    @Test
    void mergeByAddressableDelegatesToCoordinates() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 1, 1);
        map.put(pos, "A");
        map.merge(pos, "B", (oldV, newV) -> oldV + newV);
        assertEquals("AB", map.get(pos));
    }

    @Test
    void fastEntrySetIsCachedAndReflectsSize() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        var entrySet = map.fastEntrySet();
        assertSame(entrySet, map.fastEntrySet());
        assertEquals(2, entrySet.size());
    }

    @Test
    void fastEntrySetFastForEachVisitsAllEntries() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        Set<String> seen = new HashSet<>();
        map.fastEntrySet().fastForEach(e -> seen.add(e.getX() + "," + e.getY() + "," + e.getZ() + "=" + e.getValue()));

        assertEquals(new HashSet<>(Arrays.asList("1,2,3=A", "4,5,6=B")), seen);
    }

    @Test
    void fastEntrySetIteratorReturnsIndependentBoxedEntries() {
        map.put(1, 2, 3, "A");
        map.put(4, 5, 6, "B");

        var iter = map.fastEntrySet().iterator();
        Entry3D<String> first = iter.next();
        Entry3D<String> second = iter.next();

        assertNotSame(first, second);
        Set<String> seen = new HashSet<>();
        seen.add(first.getX() + "," + first.getY() + "," + first.getZ() + "=" + first.getValue());
        seen.add(second.getX() + "," + second.getY() + "," + second.getZ() + "=" + second.getValue());
        assertEquals(new HashSet<>(Arrays.asList("1,2,3=A", "4,5,6=B")), seen);
    }

    @Test
    void entry3DLongKeyConstructorUnpacksCoordinates() {
        ImmutableXYZ pos = new ImmutableXYZ(1, 2, 3);
        map.put(pos, "A");
        long key = map.fastEntryIterable().iterator().next().getLongKey();

        Entry3D<String> entry = new Entry3D<>(key, "A");
        assertEquals(1, entry.getX());
        assertEquals(2, entry.getY());
        assertEquals(3, entry.getZ());
        assertEquals("A", entry.getValue());
    }

    @Test
    void entry3DCoordinateConstructorPacksCoordinates() {
        Entry3D<String> entry = new Entry3D<>(1, 2, 3, "A");
        assertEquals(1, entry.getX());
        assertEquals(2, entry.getY());
        assertEquals(3, entry.getZ());
        assertEquals("A", entry.getValue());
    }

    @Test
    void entry3DSetValueReturnsOldValue() {
        Entry3D<String> entry = new Entry3D<>(1, 2, 3, "A");
        assertEquals("A", entry.setValue("B"));
        assertEquals("B", entry.getValue());
    }
}
