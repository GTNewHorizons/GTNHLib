package com.gtnewhorizon.gtnhlib.test.datastructs.spatialhashgrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid.DimSpatialHashGrid;
import com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid.Int3;

public class DimSpatialHashGridTest {

    DimSpatialHashGrid<TestObject> grid;

    @BeforeEach
    public void setup() {
        grid = new DimSpatialHashGrid<>(10, obj -> new Int3(obj.x, obj.y, obj.z));
    }

    @Test
    public void testInsertionAndQuery() {
        TestObject a = new TestObject("A", 5, 5, 5);
        TestObject b = new TestObject("B", 50, 50, 50);
        grid.insert(0, a);
        grid.insert(0, b);

        List<TestObject> nearby = grid.findNearby(0, 6, 5, 5, 5);
        assertEquals(1, nearby.size());
        assertEquals("A", nearby.get(0).id);
    }

    @Test
    public void testRemoval() {
        TestObject a = new TestObject("A", 5, 5, 5);
        grid.insert(0, a);

        List<TestObject> nearby = grid.findNearby(0, 5, 5, 5, 5);
        assertEquals(1, nearby.size());

        grid.remove(0, a);
        nearby = grid.findNearby(0, 5, 5, 5, 5);
        assertEquals(0, nearby.size());
    }

    @Test
    public void testQueryNoResults() {
        List<TestObject> nearby = grid.findNearby(0, 100, 100, 100, 10);
        assertTrue(nearby.isEmpty());
    }

    @Test
    public void testMultipleObjectsInRadius() {
        TestObject a = new TestObject("A", 5, 5, 5);
        TestObject b = new TestObject("B", 7, 5, 5);
        TestObject c = new TestObject("C", 20, 20, 20);
        grid.insert(0, a);
        grid.insert(0, b);
        grid.insert(0, c);

        List<TestObject> nearby = grid.findNearby(0, 6, 5, 5, 5);
        assertEquals(2, nearby.size());
    }

    @Test
    public void testDifferentDimensions() {
        TestObject a = new TestObject("A", 0, 0, 0);
        TestObject b = new TestObject("B", 0, 0, 0);

        grid.insert(0, a);
        grid.insert(1, b);

        assertEquals(1, grid.findNearby(0, 0, 0, 0, 1).size());
        assertEquals(1, grid.findNearby(1, 0, 0, 0, 1).size());
    }
}
