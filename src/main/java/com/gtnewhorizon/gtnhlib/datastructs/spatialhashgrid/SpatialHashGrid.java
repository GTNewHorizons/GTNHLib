package com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid;

import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizon.gtnhlib.util.DistanceUtil;

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

    public enum DistanceFormula {
        SquaredEuclidean,
        /**
         * Chessboard
         */
        Chebyshev,

        /**
         * Taxicab
         */
        Manhattan
    }

    public SpatialHashGrid(int cellSize, BiConsumer<Vector3i, T> positionExtractor) {
        if (cellSize <= 0) throw new IllegalArgumentException("cellSize can not be less than or equal to 0");
        this.cellSize = cellSize;
        this.positionExtractor = positionExtractor;
    }

    private long pack(int x, int y, int z) {
        return CoordinatePacker.pack(x, y, z);
    }

    private long hash(int x, int y, int z) {
        return pack(Math.floorDiv(x, cellSize), Math.floorDiv(y, cellSize), Math.floorDiv(z, cellSize));
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
     * Search the grid for nearby objects Default Distance Formula: Squared Euclidean
     *
     * @param radius distance in blocks to check (sphere)
     * @return list of nearby objects
     */
    public List<T> findNearby(int x, int y, int z, int radius) {
        return findNearby(x, y, z, radius, DistanceFormula.SquaredEuclidean);
    }

    /**
     * Search the grid for nearby objects
     *
     * @param radius          distance in blocks to check (sphere)
     * @param distanceFormula Distance Formula to use
     * @return list of nearby objects
     */
    public List<T> findNearby(int x, int y, int z, int radius, DistanceFormula distanceFormula) {
        radius = Math.abs(radius); // just no
        final int cellX = Math.floorDiv(x, cellSize);
        final int cellY = Math.floorDiv(y, cellSize);
        final int cellZ = Math.floorDiv(z, cellSize);

        // Make sure that cells which partially fall in the radius are still checked
        final int cellRad = (radius + cellSize - 1) / cellSize;
        final int radiusSquared = radius * radius;

        final ObjectArrayList<T> result = new ObjectArrayList<>();

        for (int dx = -cellRad; dx <= cellRad; dx++) {
            for (int dy = -cellRad; dy <= cellRad; dy++) {
                for (int dz = -cellRad; dz <= cellRad; dz++) {
                    long key = pack(cellX + dx, cellY + dy, cellZ + dz);
                    final ObjectArrayList<T> list = grid.get(key);
                    if (list == null || list.isEmpty()) continue;

                    boolean isEdge = (Math.abs(dx) == cellRad) || (Math.abs(dy) == cellRad)
                            || (Math.abs(dz) == cellRad);

                    if (isEdge) {
                        for (T obj : list) {
                            positionExtractor.accept(scratch, obj);
                            if (distanceBetweenPoints(x, y, z, scratch.x, scratch.y, scratch.z, distanceFormula)
                                    > radiusSquared)
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

    private double distanceBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2,
            DistanceFormula distanceFormula) {
        return switch (distanceFormula) {
            case SquaredEuclidean -> DistanceUtil.squaredEuclideanDistance(x1, y1, z1, x2, y2, z2);
            case Chebyshev -> DistanceUtil.chebyshevDistance(x1, y1, z1, x2, y2, z2);
            case Manhattan -> DistanceUtil.manhattanDistance(x1, y1, z1, x2, y2, z2);
        };
    }
}
