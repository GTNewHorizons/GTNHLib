package com.gtnewhorizon.gtnhlib.test.datastructs.spatialhashgrid;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

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

        Collection<TestObject> results = grid.findNearbyChebyshev(5, 5, 5, 1);
        assertNotNull(results);
        assertTrue(results.contains(obj));
        assertEquals(1, results.size());
    }

    @Test
    void testInsertAndQueryForumula() {
        SpatialHashGrid<TestObject> grid = new SpatialHashGrid<>(1, (pos, obj) -> {
            pos.x = obj.x;
            pos.y = obj.y;
            pos.z = obj.z;
        });

        TestObject obj = new TestObject(5, 5, 5);
        TestObject obj2 = new TestObject(1, 1, 1);
        grid.insert(obj);
        grid.insert(obj2);

        Collection<TestObject> results = grid.findNearbyManhattan(5, 5, 5, 5);
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

        Collection<TestObject> results = grid.findNearbyChebyshev(5, 5, 5, 10);
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

        Collection<TestObject> results = grid.findNearbyChebyshev(0, 0, 0, 10);
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

        Collection<TestObject> results = grid.findNearbyChebyshev(-5, -5, -5, 1);
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

        Collection<TestObject> results = grid.findNearbyChebyshev(15, 5, 5, 1);
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

        var result = grid.findClosestNearbyChebyshev(9, 5, 5, 10);
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

        var result = grid.findFirstNearbyChebyshev(6, 5, 5, 10);
        assertNotNull(result);
        assertEquals(result, obj);
    }
}
