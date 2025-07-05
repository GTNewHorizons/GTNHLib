package com.gtnewhorizon.gtnhlib.test.datastructs.space;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityCheck4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.VolumeShape;

class ArrayProximityTest {

    private ArrayProximityCheck4D sphereCheck;
    private ArrayProximityCheck4D cubeCheck;
    private ArrayProximityMap4D<String> mapSphere;
    private ArrayProximityMap4D<String> mapCube;

    @BeforeEach
    void setUp() {
        sphereCheck = new ArrayProximityCheck4D(VolumeShape.SPHERE);
        cubeCheck = new ArrayProximityCheck4D(VolumeShape.CUBE);
        mapSphere = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        mapCube = new ArrayProximityMap4D<>(VolumeShape.CUBE);
    }

    @Test
    void testPutAndIsInVolume() {
        sphereCheck.put(0, 10, 10, 10, 5);
        assertTrue(sphereCheck.isInVolume(0, 10.0, 10.0, 10.0));
        assertFalse(sphereCheck.isInVolume(0, 20.0, 20.0, 20.0));

        cubeCheck.put(1, 0, 0, 0, 1);
        assertTrue(cubeCheck.isInVolume(1, 0.5, 0.5, 0.5));
        assertFalse(cubeCheck.isInVolume(1, 3.0, 3.0, 3.0));
    }

    @Test
    void testOverwriteRadius() {
        sphereCheck.put(0, 5, 5, 5, 3);
        assertTrue(sphereCheck.isInVolume(0, 7.0, 5.0, 5.0));
        sphereCheck.put(0, 5, 5, 5, 1);
        assertFalse(sphereCheck.isInVolume(0, 7.0, 5.0, 5.0));

        cubeCheck.put(0, 5, 5, 5, 3);
        assertTrue(cubeCheck.isInVolume(0, 7.0, 5.0, 5.0));
        cubeCheck.put(0, 5, 5, 5, 1);
        assertFalse(cubeCheck.isInVolume(0, 7.0, 5.0, 5.0));
    }

    @Test
    void testRemoveVolume() {
        sphereCheck.put(0, 2, 2, 2, 2);
        assertTrue(sphereCheck.isInVolume(0, 2.0, 2.0, 2.0));
        sphereCheck.remove(0, 2, 2, 2);
        assertFalse(sphereCheck.isInVolume(0, 2.0, 2.0, 2.0));

        cubeCheck.put(0, 2, 2, 2, 2);
        assertTrue(cubeCheck.isInVolume(0, 2.0, 2.0, 2.0));
        cubeCheck.remove(0, 2, 2, 2);
        assertFalse(cubeCheck.isInVolume(0, 2.0, 2.0, 2.0));
    }

    @Test
    void testSizeAndIsEmpty() {
        assertTrue(sphereCheck.isEmpty());
        assertEquals(0, sphereCheck.size());
        sphereCheck.put(0, 1, 1, 1, 1);
        sphereCheck.put(0, 2, 2, 2, 1);
        assertFalse(sphereCheck.isEmpty());
        assertEquals(2, sphereCheck.size());
        sphereCheck.remove(0, 1, 1, 1);
        sphereCheck.remove(0, 2, 2, 2);
        assertTrue(sphereCheck.isEmpty());
        assertEquals(0, sphereCheck.size());

        assertTrue(cubeCheck.isEmpty());
        assertEquals(0, cubeCheck.size());
        cubeCheck.put(0, 1, 1, 1, 1);
        cubeCheck.put(0, 2, 2, 2, 1);
        assertFalse(cubeCheck.isEmpty());
        assertEquals(2, cubeCheck.size());
        cubeCheck.remove(0, 1, 1, 1);
        cubeCheck.remove(0, 2, 2, 2);
        assertTrue(cubeCheck.isEmpty());
        assertEquals(0, cubeCheck.size());
    }

    @Test
    void testMultipleDimensions() {
        sphereCheck.put(0, 0, 0, 0, 2);
        sphereCheck.put(1, 10, 10, 10, 3);

        assertTrue(sphereCheck.isInVolume(0, 0.0, 0.0, 0.0));
        assertTrue(sphereCheck.isInVolume(1, 10.0, 10.0, 10.0));
        assertFalse(sphereCheck.isInVolume(2, 10.0, 10.0, 10.0));

        assertEquals(2, sphereCheck.size());
    }

    @Test
    void testRemoveNonexistentVolume() {
        sphereCheck.put(0, 1, 1, 1, 1);
        sphereCheck.remove(0, 9, 9, 9);
        assertEquals(1, sphereCheck.size());
    }

    @Test
    void testPutAndSize() {
        mapSphere.put("A", 0, 10, 20, 30, 5);
        assertEquals(1, mapSphere.size());
        mapSphere.put("B", 0, 15, 25, 35, 5);
        assertEquals(2, mapSphere.size());
    }

    @Test
    void testPutReplacesSameCoords() {
        mapSphere.put("A", 0, 10, 20, 30, 5);
        mapSphere.put("B", 0, 10, 20, 30, 10);
        assertEquals(1, mapSphere.size());
        assertEquals("B", mapSphere.getClosest(0, 10.5, 20.5, 30.5));
    }

    @Test
    void testRemoveExisting() {
        mapSphere.put("A", 0, 10, 20, 30, 5);
        String removed = mapSphere.remove(0, 10, 20, 30);
        assertEquals("A", removed);
        assertEquals(0, mapSphere.size());
    }

    @Test
    void testRemoveNonexistent() {
        assertNull(mapSphere.remove(0, 1, 2, 3));
        assertNull(mapCube.remove(0, 1, 2, 3));
    }

    @Test
    void testIsInVolumeSphere() {
        mapSphere.put("Center", 0, 10, 10, 10, 5);
        assertTrue(mapSphere.isInVolume(0, 10.5, 10.5, 10.5));
        assertFalse(mapSphere.isInVolume(0, 20.0, 20.0, 20.0));
    }

    @Test
    void testIsInVolumeCube() {
        mapCube.put("Center", 0, 10, 10, 10, 5);
        assertTrue(mapCube.isInVolume(0, 11.0, 10.0, 9.0));
        assertFalse(mapCube.isInVolume(0, 20.0, 20.0, 20.0));
    }

    @Test
    void testGetClosestSphere() {
        mapSphere.put("A", 0, 5, 5, 5, 3);
        mapSphere.put("B", 0, 10, 10, 10, 3);
        assertEquals("A", mapSphere.getClosest(0, 5.5, 5.5, 5.5));
        assertEquals("B", mapSphere.getClosest(0, 10.5, 10.5, 10.5));
    }

    @Test
    void testGetClosestCube() {
        mapCube.put("A", 0, 5, 5, 5, 20);
        mapCube.put("B", 0, 10, 10, 10, 20);
        assertEquals("A", mapCube.getClosest(0, 5.2, 5.2, 5.2));
        assertEquals("B", mapCube.getClosest(0, 10.3, 10.3, 10.3));
    }

    @Test
    void testMultipleDimensionsMap() {
        mapSphere.put("Dim0", 0, 1, 1, 1, 20);
        assertTrue(mapSphere.isInVolume(0, 1.5, 1.5, 1.5));
        assertFalse(mapSphere.isInVolume(1, 1.5, 1.5, 1.5));
        mapSphere.put("Dim1", 1, 1, 1, 1, 20);
        assertEquals("Dim1", mapSphere.getClosest(1, 1.5, 1.5, 1.5));
    }

    @Test
    void testMultiRemove() {
        mapSphere.put("A", 0, 0, 0, 0, 50);
        mapSphere.put("B", 0, 10, 10, 10, 50);
        mapSphere.put("C", 0, 10, 0, 10, 50);
        mapSphere.put("D", 0, 10, -10, 10, 50);
        mapSphere.put("E", 0, -10, -10, -10, 50);

        assertEquals(mapSphere.size(), 5);
        assertEquals("D", mapSphere.getClosest(0, 11, -10, 10));

        mapSphere.remove(0, 10, -10, 10);
        assertEquals("C", mapSphere.getClosest(0, 11, -10, 10));
        mapSphere.remove(0, -10, -10, -10);
        assertEquals(3, mapSphere.size());
    }

    @Test
    void testIsEmptyAndClear() {
        assertTrue(mapSphere.isEmpty());
        mapSphere.put("Something", 0, 1, 2, 3, 4);
        assertFalse(mapSphere.isEmpty());
        mapSphere.clear();
        assertTrue(mapSphere.isEmpty());
        assertEquals(0, mapSphere.size());
    }
}
