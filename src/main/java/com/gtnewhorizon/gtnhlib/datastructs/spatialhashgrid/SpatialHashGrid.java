package com.gtnewhorizon.gtnhlib.datastructs.spatialhashgrid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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
@SuppressWarnings("unused")
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
     * Search the grid for nearby objects using Squared Euclidean Distance Formula.
     *
     * @param radius distance in blocks to check (sphere)
     * @return list of nearby objects
     */
    public List<T> findNearbySquaredEuclidean(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.SquaredEuclidean);
        List<T> resultList = new ArrayList<>();

        while (iterator.hasNext()) {
            T obj = iterator.next();
            resultList.add(obj);
        }

        return resultList;
    }

    /**
     * Search the grid for nearby objects using Chebyshev (Chessboard) Distance Formula.
     *
     * @param radius distance in blocks to check (sphere)
     * @return list of nearby objects
     */
    public List<T> findNearbyChebyshev(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.Chebyshev);
        List<T> resultList = new ArrayList<>();

        while (iterator.hasNext()) {
            T obj = iterator.next();
            resultList.add(obj);
        }

        return resultList;
    }

    /**
     * Search the grid for nearby objects using Manhattan (Taxicab) Distance Formula.
     *
     * @param radius distance in blocks to check (sphere)
     * @return list of nearby objects
     */
    public List<T> findNearbyManhattan(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.Manhattan);
        List<T> resultList = new ArrayList<>();

        while (iterator.hasNext()) {
            T obj = iterator.next();
            resultList.add(obj);
        }

        return resultList;
    }

    /**
     * Find the first nearby object using Squared Euclidean Distance Formula.
     *
     * @param x      position to check
     * @param y      position to check
     * @param z      position to check
     * @param radius distance in blocks to check (sphere)
     * @return the first nearby object or null if none found
     */
    public T findFirstNearbySquaredEuclidean(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.SquaredEuclidean);
        return (iterator.hasNext() ? iterator.next() : null);
    }

    /**
     * Find the first nearby object using Chebyshev (Chessboard) Distance Formula.
     *
     * @param x      position to check
     * @param y      position to check
     * @param z      position to check
     * @param radius distance in blocks to check (sphere)
     * @return the first nearby object or null if none found
     */
    public T findFirstNearbyChebyshev(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.Chebyshev);
        return (iterator.hasNext() ? iterator.next() : null);
    }

    /**
     * Find the first nearby object using Manhattan (Taxicab) Distance Formula.
     *
     * @param x      position to check
     * @param y      position to check
     * @param z      position to check
     * @param radius distance in blocks to check (sphere)
     * @return the first nearby object or null if none found
     */
    public T findFirstNearbyManhattan(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.Manhattan);
        return (iterator.hasNext() ? iterator.next() : null);
    }

    /**
     * Find the closest nearby object using Squared Euclidean Distance Formula.
     *
     * @param x      position to check
     * @param y      position to check
     * @param z      position to check
     * @param radius distance in blocks to check (sphere)
     * @return the closest nearby object or null if none found
     */
    public T findClosestNearbySquaredEuclidean(int x, int y, int z, int radius) {
        T closestObject = null;
        double closestDistance = Double.MAX_VALUE;

        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.SquaredEuclidean);
        while (iterator.hasNext()) {
            T obj = iterator.next();
            positionExtractor.accept(scratch, obj);
            double distance = distanceBetweenPoints(
                    x,
                    y,
                    z,
                    scratch.x,
                    scratch.y,
                    scratch.z,
                    DistanceFormula.SquaredEuclidean);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestObject = obj;
            }
        }

        return closestObject;
    }

    /**
     * Find the closest nearby object using Chebyshev (Chessboard) Distance Formula.
     *
     * @param x      position to check
     * @param y      position to check
     * @param z      position to check
     * @param radius distance in blocks to check (sphere)
     * @return the closest nearby object or null if none found
     */
    public T findClosestNearbyChebyshev(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.Chebyshev);
        T closestObject = null;
        double closestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            T obj = iterator.next();
            positionExtractor.accept(scratch, obj);
            double distance = distanceBetweenPoints(
                    x,
                    y,
                    z,
                    scratch.x,
                    scratch.y,
                    scratch.z,
                    DistanceFormula.Chebyshev);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestObject = obj;
            }
        }

        return closestObject;
    }

    /**
     * Find the closest nearby object using Manhattan (Taxicab) Distance Formula.
     *
     * @param x      position to check
     * @param y      position to check
     * @param z      position to check
     * @param radius distance in blocks to check (sphere)
     * @return the closest nearby object or null if none found
     */
    public T findClosestNearbyManhattan(int x, int y, int z, int radius) {
        Iterator<T> iterator = findNearbyWithFormula(x, y, z, radius, DistanceFormula.Manhattan);
        T closestObject = null;
        double closestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            T obj = iterator.next();
            positionExtractor.accept(scratch, obj);
            double distance = distanceBetweenPoints(
                    x,
                    y,
                    z,
                    scratch.x,
                    scratch.y,
                    scratch.z,
                    DistanceFormula.Manhattan);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestObject = obj;
            }
        }

        return closestObject;
    }

    /**
     * Search the grid for nearby objects using a specified distance formula.
     *
     * @param x               position to check
     * @param y               position to check
     * @param z               position to check
     * @param radius          distance in blocks to check (sphere)
     * @param distanceFormula Distance Formula to use
     * @return iterator of nearby objects
     */
    public Iterator<T> findNearbyWithFormula(int x, int y, int z, int radius, DistanceFormula distanceFormula) {
        radius = Math.abs(radius); // just no
        final int cellX = Math.floorDiv(x, cellSize);
        final int cellY = Math.floorDiv(y, cellSize);
        final int cellZ = Math.floorDiv(z, cellSize);

        // Make sure that cells which partially fall in the radius are still checked
        final int cellRad = (radius + cellSize - 1) / cellSize;
        final int distanceCompared = (distanceFormula == DistanceFormula.SquaredEuclidean ? radius * radius : radius);

        return new Iterator<T>() {

            private int dx = -cellRad, dy = -cellRad, dz = -cellRad;
            private ObjectArrayList<T> currentList;

            @Override
            public boolean hasNext() {
                if (currentList != null && !currentList.isEmpty()) {
                    return true;
                }

                while (dx <= cellRad) {
                    while (dy <= cellRad) {
                        while (dz <= cellRad) {
                            long key = pack(cellX + dx, cellY + dy, cellZ + dz);
                            currentList = grid.get(key);
                            if (currentList != null && !currentList.isEmpty()) {
                                return true;
                            }

                            dz++;
                        }
                        dz = -cellRad; // Reset dz
                        dy++;
                    }
                    dy = -cellRad; // Reset dy
                    dx++;
                }

                return false;
            }

            @Override
            public T next() {
                if (currentList != null && !currentList.isEmpty()) {
                    T obj = currentList.get(currentList.size() - 1);
                    currentList.remove(obj);
                    positionExtractor.accept(scratch, obj);
                    boolean isEdge = (Math.abs(dx) == cellRad) || (Math.abs(dy) == cellRad)
                            || (Math.abs(dz) == cellRad);
                    if (isEdge && distanceBetweenPoints(x, y, z, scratch.x, scratch.y, scratch.z, distanceFormula)
                            > distanceCompared) {
                        return next(); // Skip this object and continue
                    }
                    return obj;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
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
