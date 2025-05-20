package com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * 3D Spatial Grid Per Dimension Objects are hashed using their position
 *
 * @param <T> object type to store
 */
public class DimSpatialHashGrid<T> {

    private final int cellSize;
    private final Position3i<T> positionExtractor;
    private final Int2ObjectOpenHashMap<Long2ObjectOpenHashMap<ObjectArrayList<T>>> gridByDimension = new Int2ObjectOpenHashMap<>();

    public DimSpatialHashGrid(int cellSize, Position3i<T> positionExtractor) {
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
     * 
     * @param dimensionId
     * @param obj
     */
    public void insert(int dimensionId, T obj) {
        var pos = positionExtractor.getPosition(obj);
        long key = hash(pos.x(), pos.y(), pos.z());
        var grid = gridByDimension.computeIfAbsent(dimensionId, d -> new Long2ObjectOpenHashMap<>());
        ObjectArrayList<T> list = grid.computeIfAbsent(key, k -> new ObjectArrayList<>());
        list.add(obj);
    }

    /**
     * Remove an object for the grid
     * 
     * @param dimensionId
     * @param obj
     */
    public void remove(int dimensionId, T obj) {
        var pos = positionExtractor.getPosition(obj);
        long key = hash(pos.x(), pos.y(), pos.z());
        var grid = gridByDimension.get(dimensionId);
        if (grid != null) {
            ObjectArrayList<T> list = grid.get(key);
            if (list != null) {
                list.remove(obj);
                if (list.isEmpty()) {
                    grid.remove(key);
                }
            }
        }
    }

    /**
     * Search the grid for nearby objects
     * 
     * @param dimensionId
     * @param x
     * @param y
     * @param z
     * @param radius
     * @return list of nearby objects
     */
    public List<T> findNearby(int dimensionId, int x, int y, int z, int radius) {
        var dimMap = gridByDimension.get(dimensionId);
        if (dimMap == null) return Collections.emptyList();

        int rCells = (int) Math.ceil((double) radius / cellSize);
        int gx = x / cellSize;
        int gy = y / cellSize;
        int gz = z / cellSize;

        List<T> result = new ArrayList<>();

        for (int dx = -rCells; dx <= rCells; dx++) {
            for (int dy = -rCells; dy <= rCells; dy++) {
                for (int dz = -rCells; dz <= rCells; dz++) {
                    long key = pack(gx + dx, gy + dy, gz + dz);
                    ObjectArrayList<T> list = dimMap.get(key);
                    if (list != null) {
                        for (T obj : list) {
                            Int3 pos = positionExtractor.getPosition(obj);
                            if (distanceSquared(x, y, z, pos.x(), pos.y(), pos.z()) <= radius * radius) {
                                result.add(obj);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }
}
