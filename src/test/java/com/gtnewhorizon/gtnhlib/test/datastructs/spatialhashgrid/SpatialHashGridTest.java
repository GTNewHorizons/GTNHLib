package com.gtnewhorizon.gtnhlib.test.datastructs.spatialhashgrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid.SpatialHashGrid;

public class SpatialHashGridTest {

    @Test
    void testInsertAndQuery() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(5, 5, 5);
        grid.insert(obj);

        Collection<TestObject> results = grid.collectNearbyChebyshev(5, 5, 5, 1);
        assertNotNull(results);
        assertTrue(results.contains(obj));
        assertEquals(1, results.size());
    }

    @Test
    void testInsertAndQueryFormula() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(1, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(5, 5, 5);
        TestObject obj2 = new TestObject(1, 1, 1);
        grid.insert(obj);
        grid.insert(obj2);

        Collection<TestObject> results = grid.collectNearbyManhattan(5, 5, 5, 12);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.contains(obj));
        assertTrue(results.contains(obj2));
    }

    @Test
    void testMultipleInserts() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj1 = new TestObject(5, 5, 5);
        TestObject obj2 = new TestObject(3, 3, 3);
        grid.insert(obj1);
        grid.insert(obj2);

        Collection<TestObject> results = grid.collectNearbyChebyshev(5, 5, 5, 10);
        assertNotNull(results);
        assertTrue(results.contains(obj1));
        assertTrue(results.contains(obj2));
        assertEquals(2, results.size());
    }

    @Test
    void testEmptyGrid() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        Collection<TestObject> results = grid.collectNearbyChebyshev(0, 0, 0, 10);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testNegativeCoordinates() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(-5, -5, -5);
        grid.insert(obj);

        Collection<TestObject> results = grid.collectNearbyChebyshev(-5, -5, -5, 1);
        assertNotNull(results);
        assertTrue(results.contains(obj));
        assertEquals(1, results.size());
    }

    @Test
    void testObjectMove() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(5, 5, 5);
        grid.insert(obj);

        grid.remove(obj);
        obj.x = 15;
        grid.insert(obj);

        Collection<TestObject> results = grid.collectNearbyChebyshev(15, 5, 5, 1);
        assertNotNull(results);
        assertTrue(results.contains(obj));
        assertEquals(1, results.size());
    }

    @Test
    void testClosest() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(5, 5, 5);
        grid.insert(obj);
        TestObject obj2 = new TestObject(10, 5, 5);
        grid.insert(obj2);

        TestObject result = grid.findClosestChebyshev(9, 5, 5, 10);
        assertNotNull(result);
        assertEquals(result, obj2);
    }

    @Test
    void testFirst() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(5, 5, 5);
        grid.insert(obj);
        TestObject obj2 = new TestObject(10, 5, 5);
        grid.insert(obj2);

        TestObject result = grid.findFirstNearbyChebyshev(6, 5, 5, 10);
        assertNotNull(result);
        assertEquals(result, obj);
    }

    @Test
    void testZeroRadiusIncludesExactMatch() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(1, 2, 3);
        grid.insert(obj);

        List<TestObject> result = grid.collectNearbySquaredEuclidean(1, 2, 3, 0);
        assertEquals(1, result.size());
        assertTrue(result.contains(obj));
    }

    @Test
    void testOutsideRadiusExcluded() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(5, 5, 5);
        grid.insert(obj);

        List<TestObject> result = grid.collectNearbySquaredEuclidean(5, 5, 5 + 2, 1); // too far (dist² = 4 > 1²)
        assertTrue(result.isEmpty());
    }

    @Test
    void testIteratorCorrectness() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(2, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(0, 0, 0);
        grid.insert(obj);

        Iterator<TestObject> it = grid.iterNearbyWithMetric(0, 0, 0, 5, SpatialHashGrid.DistanceFormula.Manhattan);
        assertTrue(it.hasNext());
        assertEquals(obj, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testMassInsertAndRemove() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(3, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        List<TestObject> all = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            TestObject obj = new TestObject(i, i % 10, i % 5);
            all.add(obj);
            grid.insert(obj);
        }

        for (TestObject obj : all) {
            assertTrue(grid.collectNearbyChebyshev(obj.x, obj.y, obj.z, 0).contains(obj));
            grid.remove(obj);
        }

        // Everything should now be gone
        assertTrue(grid.collectNearbyChebyshev(0, 0, 0, 1000).isEmpty());
    }

    @Test
    void testObjectsOnCellBoundaries() {
        // cellSize = 5 means cells run [0..4], [5..9], etc. for non-negative coordinates.
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(5, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        // Place objects exactly at (5,0,0) and (0,5,0) (both are cell boundaries).
        TestObject onXBoundary = new TestObject(5, 0, 0);
        TestObject onYBoundary = new TestObject(0, 5, 0);
        grid.insert(onXBoundary);
        grid.insert(onYBoundary);

        // Query at exactly (5,0,0) with radius=0 (Squared Euclidean)
        Collection<TestObject> eucResults = grid.collectNearbySquaredEuclidean(5, 0, 0, 0);
        assertTrue(eucResults.contains(onXBoundary));
        assertEquals(1, eucResults.size());

        // Query at (4,0,0) with radius=1 (should include (5,0,0) in squared Euclidean)
        eucResults = grid.collectNearbySquaredEuclidean(4, 0, 0, 1);
        assertTrue(eucResults.contains(onXBoundary));
        assertFalse(eucResults.contains(onYBoundary));
        assertEquals(1, eucResults.size());

        // Query at (5,1,0) with radius=1 Manhattan: |5−5|+|1−0|+|0−0|=1 => include onXBoundary
        Collection<TestObject> manResults = grid.collectNearbyManhattan(5, 1, 0, 1);
        assertTrue(manResults.contains(onXBoundary));
        assertFalse(manResults.contains(onYBoundary));

        // Query at (1,5,0) with radius=1 Chebyshev: max(|1−0|,|5−5|,0)=1 => include onYBoundary
        Collection<TestObject> chebResults = grid.collectNearbyChebyshev(1, 5, 0, 1);
        assertTrue(chebResults.contains(onYBoundary));
        assertFalse(chebResults.contains(onXBoundary));
    }

    @Test
    void testMetricDifference() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(1, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        // Place a single object at (2,2,2)
        TestObject obj = new TestObject(2, 2, 2);
        grid.insert(obj);

        // Query from (0,0,0) with radius=3:
        // ‣ Squared Euclidean: dist²(0→2) = 4+4+4 =12 → 12 > 9 → exclude
        Collection<TestObject> sqResults = grid.collectNearbySquaredEuclidean(0, 0, 0, 3);
        assertTrue(sqResults.isEmpty());

        // ‣ Manhattan: |2|+|2|+|2| = 6 → 6 > 3 → exclude
        Collection<TestObject> manResults = grid.collectNearbyManhattan(0, 0, 0, 3);
        assertTrue(manResults.isEmpty());

        // ‣ Chebyshev: max(|2|,|2|,|2|) = 2 → 2 ≤ 3 → include
        Collection<TestObject> chebResults = grid.collectNearbyChebyshev(0, 0, 0, 3);
        assertTrue(chebResults.contains(obj));
        assertEquals(1, chebResults.size());
    }

    @Test
    void testZeroRadiusManhattanAndChebyshev() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(2, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject exactly = new TestObject(4, 4, 4);
        TestObject near1 = new TestObject(4, 5, 4);
        TestObject near2 = new TestObject(3, 4, 4);

        grid.insert(exactly);
        grid.insert(near1);
        grid.insert(near2);

        // Radius 0 (Manhattan): only (4,4,4) has L1=0
        assertEquals(Collections.singleton(exactly), new HashSet<>(grid.collectNearbyManhattan(4, 4, 4, 0)));

        // Radius 0 (Chebyshev): only (4,4,4) has L∞=0
        assertEquals(Collections.singleton(exactly), new HashSet<>(grid.collectNearbyChebyshev(4, 4, 4, 0)));
    }

    @Test
    void testLargeRadiusSpanningManyCells() {
        int cellSize = 10;
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(cellSize, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        List<TestObject> inserted = new ArrayList<>();
        // Insert objects at (±i, 0, 0), for i from 0..50
        for (int i = -50; i <= 50; i += 10) {
            TestObject o = new TestObject(i, 0, 0);
            inserted.add(o);
            grid.insert(o);
        }

        // Query around (0,0,0) with radius = 45 (SquaredEuclidean means R²=2025)
        // Only objects with |x| ≤ 45 should be included (i.e. at -40, -30, -20, -10, 0, 10, 20, 30, 40)
        Collection<TestObject> eucResults = grid.collectNearbySquaredEuclidean(0, 0, 0, 45);
        Set<TestObject> expected = new HashSet<>();
        for (TestObject o : inserted) {
            if (o.x * o.x + o.y * o.y + o.z * o.z <= 45 * 45) {
                expected.add(o);
            }
        }
        assertEquals(expected, new HashSet<>(eucResults));

        // Query Chebyshev with radius=25: that includes all objects with max(|x|,0,0) ≤ 25 → |x| ≤ 25
        Collection<TestObject> chebResults = grid.collectNearbyChebyshev(0, 0, 0, 25);
        expected.clear();
        for (TestObject o : inserted) {
            if (Math.abs(o.x) <= 25) expected.add(o);
        }
        assertEquals(expected.size(), chebResults.size());
        assertTrue(chebResults.containsAll(expected));
    }
}
