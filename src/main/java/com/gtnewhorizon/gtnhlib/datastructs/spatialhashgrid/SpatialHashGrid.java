package com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid;

import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * 3D Spatial Grid <br>
 * Objects are hashed using their position
 *
 * @param <T> object type to store
 */
public class SpatialHashGrid<T> {

    private final int cellSize;
    private final BiConsumer<Vector3i, T> positionExtractor;
    private final Vector3i scratch = new Vector3i();
    private final Long2ObjectOpenHashMap<ObjectArrayList<T>> grid = new Long2ObjectOpenHashMap<>();

    public SpatialHashGrid(int cellSize, BiConsumer<Vector3i, T> positionExtractor) {
        this.cellSize = cellSize;
        this.positionExtractor = positionExtractor;
    }

    private long pack(int x, int y, int z) {
        return CoordinatePacker.pack(x, y, z);
    }

    private long hash(int x, int y, int z) {
        return pack(x / cellSize, y / cellSize, z / cellSize);
    }

    /**
     * Insert an object into the grid
     */
    public void insert(T obj) {
        positionExtractor.accept(scratch, obj);
        long key = hash(scratch.x, scratch.y, scratch.z);
        ObjectArrayList<T> list = grid.computeIfAbsent(key, k -> new ObjectArrayList<>());
        list.add(obj);
    }

    /**
     * Remove an object for the grid
     */
    public void remove(T obj) {
        positionExtractor.accept(scratch, obj);
        long key = hash(scratch.x, scratch.y, scratch.z);
        ObjectArrayList<T> list = grid.get(key);
        if (list == null) return;

        list.remove(obj);
        if (!list.isEmpty()) return;

        grid.remove(key);
    }

    /**
     * Search the grid for nearby objects
     *
     * @param radius
     * @return list of nearby objects
     */
    public List<T> findNearby(int x, int y, int z, int radius) {
        final int cellX = x / cellSize;
        final int cellY = y / cellSize;
        final int cellZ = z / cellSize;

        // Make sure that cells which partially fall in the radius are still checked
        final int cellRad = (radius + cellSize - 1) / cellSize;
        final int radiusSquared = radius * radius;

        final ObjectArrayList<T> result = new ObjectArrayList<>();

        for (int dx = -cellRad; dx <= cellRad; dx++) {
            for (int dy = -cellRad; dy <= cellRad; dy++) {
                for (int dz = -cellRad; dz <= cellRad; dz++) {
                    long key = pack(cellX + dx, cellY + dy, cellZ + dz);
                    final ObjectArrayList<T> list = grid.get(key);
                    if (list == null) continue;

                    boolean isEdge = (Math.abs(dx) == cellRad) || (Math.abs(dy) == cellRad)
                            || (Math.abs(dz) == cellRad);

                    if (isEdge) {
                        for (T obj : list) {
                            positionExtractor.accept(scratch, obj);
                            if (distanceBetweenPoints(x, y, z, scratch.x, scratch.y, scratch.z) > radiusSquared)
                                continue;

                            result.add(obj);
                        }
                    } else {
                        result.addAll(list);
                    }
                }
            }
        }

        return result;
    }

    private double distanceBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }
}
