package com.gtnewhorizon.gtnhlib.test.datastructs.space;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.space.HashMap2D;
import com.gtnewhorizon.gtnhlib.datastructs.space.HashMap2D.Entry2D;
import com.gtnewhorizon.gtnhlib.space.ImmutableXZ;

class HashMap2DTest {

    private HashMap2D<String> map;

    @BeforeEach
    void setUp() {
        map = new HashMap2D<>();
    }

    @Test
    void putAndGetByCoordinates() {
        assertNull(map.put(1, 2, "A"));
        assertEquals("A", map.get(1, 2));
        assertNull(map.get(2, 1));
    }

    @Test
    void putAndGetByAddressable() {
        ImmutableXZ pos = new ImmutableXZ(3, 4);
        map.put(pos, "B");
        assertEquals("B", map.get(pos));
        assertEquals("B", map.get(3, 4));
    }

    @Test
    void containsKey() {
        assertFalse(map.containsKey(1, 1));
        map.put(1, 1, "A");
        assertTrue(map.containsKey(1, 1));
        assertTrue(map.containsKey(new ImmutableXZ(1, 1)));
    }

    @Test
    void removeByCoordinatesReturnsOldValue() {
        map.put(5, 6, "A");
        assertEquals("A", map.remove(5, 6));
        assertNull(map.get(5, 6));
        assertNull(map.remove(5, 6));
    }

    @Test
    void removeByAddressable() {
        ImmutableXZ pos = new ImmutableXZ(7, 8);
        map.put(pos, "A");
        assertEquals("A", map.remove(pos));
        assertFalse(map.containsKey(pos));
    }

    @Test
    void removeWithValueMatchOnlyRemovesOnMatch() {
        map.put(1, 1, "A");
        assertFalse(map.remove(1, 1, "B"));
        assertEquals("A", map.get(1, 1));
        assertTrue(map.remove(1, 1, "A"));
        assertNull(map.get(1, 1));
    }

    @Test
    void computeIfAbsentComputesOnceAndReusesExisting() {
        assertEquals("A", map.computeIfAbsent(1, 1, (x, z) -> "A"));
        assertEquals("A", map.computeIfAbsent(1, 1, (x, z) -> "B"));
    }

    @Test
    void computeAppliesRemappingFunctionWithCoordinates() {
        map.put(1, 1, "A");
        String result = map.compute(1, 1, (x, z, v) -> v + x + z);
        assertEquals("A11", result);
        assertEquals("A11", map.get(1, 1));
    }

    @Test
    void computeIfPresentSkipsMissingKeys() {
        assertNull(map.computeIfPresent(1, 1, (x, z, v) -> "shouldNotRun"));
        map.put(1, 1, "A");
        assertEquals("A!", map.computeIfPresent(1, 1, (x, z, v) -> v + "!"));
    }

    @Test
    void getOrDefaultReturnsDefaultWhenMissing() {
        assertEquals("D", map.getOrDefault(1, 1, "D"));
        map.put(1, 1, "A");
        assertEquals("A", map.getOrDefault(1, 1, "D"));
    }

    @Test
    void putIfAbsentTreatsNullValueAsPresent() {
        assertNull(map.putIfAbsent(1, 1, null));
        assertTrue(map.containsKey(1, 1));
        assertNull(map.putIfAbsent(1, 1, "A"));
        assertNull(map.get(1, 1));
    }

    @Test
    void replaceOnlyAffectsExistingKeys() {
        assertNull(map.replace(1, 1, "A"));
        assertFalse(map.containsKey(1, 1));
        map.put(1, 1, "A");
        assertEquals("A", map.replace(1, 1, "B"));
        assertEquals("B", map.get(1, 1));
    }

    @Test
    void replaceWithOldValueMatchOnlyReplacesOnMatch() {
        map.put(1, 1, "A");
        assertFalse(map.replace(1, 1, "WRONG", "B"));
        assertEquals("A", map.get(1, 1));
        assertTrue(map.replace(1, 1, "A", "B"));
        assertEquals("B", map.get(1, 1));
    }

    @Test
    void mergeCombinesValues() {
        map.put(1, 1, "A");
        map.merge(1, 1, "B", (oldV, newV) -> oldV + newV);
        assertEquals("AB", map.get(1, 1));
    }

    @Test
    void forEachVisitsAllEntriesWithCoordinates() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        Set<String> seen = new HashSet<>();
        map.forEach((x, z, v) -> seen.add(x + "," + z + "=" + v));

        Set<String> expected = new HashSet<>(Arrays.asList("1,2=A", "3,4=B"));
        assertEquals(expected, seen);
    }

    @Test
    void replaceAllRewritesValuesBasedOnCoordinates() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        map.replaceAll((x, z, v) -> v + x + z);

        assertEquals("A12", map.get(1, 2));
        assertEquals("B34", map.get(3, 4));
    }

    @Test
    void fastEntryIterableExposesCoordinatesAndValues() {
        map.put(1, 2, "A");

        var entries = map.fastEntryIterable().iterator();
        assertTrue(entries.hasNext());
        var entry = entries.next();
        assertEquals(1, entry.getX());
        assertEquals(2, entry.getZ());
        assertEquals("A", entry.getValue());
        assertFalse(entries.hasNext());
    }

    @Test
    void slowIteratorReturnsIndependentEntries() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        var iter = map.slowIterator();
        Entry2D<String> first = iter.next();
        Entry2D<String> second = iter.next();

        assertNotSame(first, second);
        Set<String> seen = new HashSet<>();
        seen.add(first.getX() + "," + first.getZ() + "=" + first.getValue());
        seen.add(second.getX() + "," + second.getZ() + "=" + second.getValue());
        assertEquals(new HashSet<>(Arrays.asList("1,2=A", "3,4=B")), seen);
    }

    @Test
    void slowStreamCountMatchesSize() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        assertEquals(2, map.slowStream().count());
    }

    @Test
    void slowStreamElementsSurviveBuffering() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        // sorted() buffers every element before emitting; slowStream must not share mutable state
        // across entries the way fastEntryStream() used to, or every entry would collapse to the last one.
        Set<String> collected = map.slowStream().sorted(Comparator.comparingInt(Entry2D::getX))
                .map(e -> e.getX() + "," + e.getZ() + "=" + e.getValue())
                .collect(Collectors.toCollection(HashSet::new));

        assertEquals(new HashSet<>(Arrays.asList("1,2=A", "3,4=B")), collected);
    }

    @Test
    void negativeCoordinatesAreDistinctFromPositive() {
        map.put(-1, -2, "Neg");
        map.put(1, 2, "Pos");

        assertEquals("Neg", map.get(-1, -2));
        assertEquals("Pos", map.get(1, 2));
        assertEquals(2, map.size());
    }

    @Test
    void computeIfAbsentByAddressableDelegatesToCoordinates() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        assertEquals("A", map.computeIfAbsent(pos, (x, z) -> "A"));
        assertEquals("A", map.computeIfAbsent(pos, (x, z) -> "B"));
    }

    @Test
    void computeByAddressableDelegatesToCoordinates() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        map.put(pos, "A");
        assertEquals("A11", map.compute(pos, (x, z, v) -> v + x + z));
    }

    @Test
    void computeIfPresentByAddressableDelegatesToCoordinates() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        assertNull(map.computeIfPresent(pos, (x, z, v) -> "shouldNotRun"));
        map.put(pos, "A");
        assertEquals("A!", map.computeIfPresent(pos, (x, z, v) -> v + "!"));
    }

    @Test
    void getOrDefaultByAddressableDelegatesToCoordinates() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        assertEquals("D", map.getOrDefault(pos, "D"));
        map.put(pos, "A");
        assertEquals("A", map.getOrDefault(pos, "D"));
    }

    @Test
    void putIfAbsentByAddressableDelegatesToCoordinates() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        assertNull(map.putIfAbsent(pos, "A"));
        assertEquals("A", map.putIfAbsent(pos, "B"));
        assertEquals("A", map.get(pos));
    }

    @Test
    void removeWithValueMatchByAddressable() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        map.put(pos, "A");
        assertFalse(map.remove(pos, "B"));
        assertTrue(map.remove(pos, "A"));
        assertNull(map.get(pos));
    }

    @Test
    void replaceByAddressable() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        assertNull(map.replace(pos, "A"));
        map.put(pos, "A");
        assertEquals("A", map.replace(pos, "B"));
        assertEquals("B", map.get(pos));
    }

    @Test
    void replaceWithOldValueMatchByAddressable() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        map.put(pos, "A");
        assertFalse(map.replace(pos, "WRONG", "B"));
        assertTrue(map.replace(pos, "A", "B"));
        assertEquals("B", map.get(pos));
    }

    @Test
    void mergeByAddressableDelegatesToCoordinates() {
        ImmutableXZ pos = new ImmutableXZ(1, 1);
        map.put(pos, "A");
        map.merge(pos, "B", (oldV, newV) -> oldV + newV);
        assertEquals("AB", map.get(pos));
    }

    @Test
    void fastEntrySetIsCachedAndReflectsSize() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        var entrySet = map.fastEntrySet();
        assertSame(entrySet, map.fastEntrySet());
        assertEquals(2, entrySet.size());
    }

    @Test
    void fastEntrySetFastForEachVisitsAllEntries() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        Set<String> seen = new HashSet<>();
        map.fastEntrySet().fastForEach(e -> seen.add(e.getX() + "," + e.getZ() + "=" + e.getValue()));

        assertEquals(new HashSet<>(Arrays.asList("1,2=A", "3,4=B")), seen);
    }

    @Test
    void fastEntrySetIteratorReturnsIndependentBoxedEntries() {
        map.put(1, 2, "A");
        map.put(3, 4, "B");

        var iter = map.fastEntrySet().iterator();
        Entry2D<String> first = iter.next();
        Entry2D<String> second = iter.next();

        assertNotSame(first, second);
        Set<String> seen = new HashSet<>();
        seen.add(first.getX() + "," + first.getZ() + "=" + first.getValue());
        seen.add(second.getX() + "," + second.getZ() + "=" + second.getValue());
        assertEquals(new HashSet<>(Arrays.asList("1,2=A", "3,4=B")), seen);
    }

    @Test
    void entry2DLongKeyConstructorUnpacksCoordinates() {
        ImmutableXZ pos = new ImmutableXZ(1, 2);
        map.put(pos, "A");
        long key = map.fastEntryIterable().iterator().next().getLongKey();

        Entry2D<String> entry = new Entry2D<>(key, "A");
        assertEquals(1, entry.getX());
        assertEquals(2, entry.getZ());
        assertEquals("A", entry.getValue());
    }

    @Test
    void entry2DCoordinateConstructorPacksCoordinates() {
        Entry2D<String> entry = new Entry2D<>(1, 2, "A");
        assertEquals(1, entry.getX());
        assertEquals(2, entry.getZ());
        assertEquals("A", entry.getValue());
    }

    @Test
    void entry2DSetValueReturnsOldValue() {
        Entry2D<String> entry = new Entry2D<>(1, 2, "A");
        assertEquals("A", entry.setValue("B"));
        assertEquals("B", entry.getValue());
    }
}
