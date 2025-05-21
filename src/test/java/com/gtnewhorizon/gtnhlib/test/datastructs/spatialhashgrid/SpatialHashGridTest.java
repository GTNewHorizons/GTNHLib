package com.gtnewhorizon.gtnhlib.test.datastructs.spatialhashgrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid.Int3;
import com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid.SpatialHashGrid;

public class SpatialHashGridTest {

    SpatialHashGrid<TestObject> grid;

    @BeforeEach
    public void setup() {
        grid = new SpatialHashGrid<>(10, obj -> new Int3(obj.x, obj.y, obj.z));
    }

    @Test
    public void testInsertionAndQuery() {
        TestObject a = new TestObject("A", 5, 5, 5);
        TestObject b = new TestObject("B", 50, 50, 50);
        grid.insert(a);
        grid.insert(b);

        List<TestObject> nearby = grid.findNearby(6, 5, 5, 5);
        assertEquals(1, nearby.size());
        assertEquals("A", nearby.get(0).id);
    }

    @Test
    public void testRemoval() {
        TestObject a = new TestObject("A", 5, 5, 5);
        grid.insert(a);

        List<TestObject> nearby = grid.findNearby(5, 5, 5, 5);
        assertEquals(1, nearby.size());

        grid.remove(a);
        nearby = grid.findNearby(5, 5, 5, 5);
        assertEquals(0, nearby.size());
    }

    @Test
    public void testQueryNoResults() {
        List<TestObject> nearby = grid.findNearby(100, 100, 100, 10);
        assertTrue(nearby.isEmpty());
    }

    @Test
    public void testMultipleObjectsInRadius() {
        TestObject a = new TestObject("A", 5, 5, 5);
        TestObject b = new TestObject("B", 7, 5, 5);
        TestObject c = new TestObject("C", 20, 20, 20);
        grid.insert(a);
        grid.insert(b);
        grid.insert(c);

        List<TestObject> nearby = grid.findNearby(6, 5, 5, 5);
        assertEquals(2, nearby.size());
    }
}
