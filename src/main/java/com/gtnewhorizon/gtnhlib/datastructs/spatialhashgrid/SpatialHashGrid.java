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

    public enum DistanceFormula implements DistanceMetric {

        SquaredEuclidean(new DistanceMetric() {

            @Override
            public double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
                return DistanceUtil.squaredEuclideanDistance(x1, y1, z1, x2, y2, z2);
            }

            @Override
            public double transformCompareDistance(double radius) {
                return radius * radius;
            }
        }),
        /**
         * Chessboard
         */
        Chebyshev(DistanceUtil::chebyshevDistance),

        /**
         * Taxicab
         */
        Manhattan(DistanceUtil::manhattanDistance);

        private final DistanceMetric metric;

        DistanceFormula(DistanceMetric metric) {
            this.metric = metric;
        }

        @Override
        public double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
            return this.metric.distance(x1, y1, z1, x2, y2, z2);
        }

        @Override
        public double transformCompareDistance(double radius) {
            return this.metric.transformCompareDistance(radius);
        }
    }

    @FunctionalInterface
    public interface DistanceMetric {

        double distance(double x1, double y1, double z1, double x2, double y2, double z2);

        default double transformCompareDistance(double radius) {
            return radius;
        }
    }

    public SpatialHashGrid(int cellSize, BiConsumer<Vector3i, T> positionExtractor) {
        if (cellSize <= 0) throw new IllegalArgumentException("cellSize can not be less than or equal to 0");
        this.cellSize = cellSize;
        this.positionExtractor = positionExtractor;
    }

    private static long pack(int x, int y, int z) {
        return CoordinatePacker.pack(x, y, z);
    }

    private static long hash(int cellSize, int x, int y, int z) {
        return pack(Math.floorDiv(x, cellSize), Math.floorDiv(y, cellSize), Math.floorDiv(z, cellSize));
    }

    /**
     * Insert an object into the grid
     */
    public void insert(T obj) {
        positionExtractor.accept(scratch, obj);
        long key = hash(cellSize, scratch.x, scratch.y, scratch.z);
        ObjectArrayList<T> list = grid.computeIfAbsent(key, k -> new ObjectArrayList<>());
        list.add(obj);
    }

    /**
     * Remove an object for the grid
     */
    public void remove(T obj) {
        positionExtractor.accept(scratch, obj);
        long key = hash(cellSize, scratch.x, scratch.y, scratch.z);
        ObjectArrayList<T> list = grid.get(key);
        if (list == null) return;

        list.remove(obj);
        if (!list.isEmpty()) return;

        grid.remove(key);
    }

    public boolean contains(T obj) {
        positionExtractor.accept(scratch, obj);
        long key = hash(cellSize, scratch.x, scratch.y, scratch.z);
        ObjectArrayList<T> list = grid.get(key);
        if (list == null) return false;

        return list.contains(obj);
    }

    /**
     * Search the grid for nearby objects using Squared Euclidean Distance Formula.
     *
     * @param radius distance in blocks to check (sphere)
     * @return list of nearby objects
     */
    public List<T> collectNearbySquaredEuclidean(int x, int y, int z, int radius) {
        return collectNearbyWithMetric(x, y, z, radius, DistanceFormula.SquaredEuclidean);
    }

    /**
     * Search the grid for nearby objects using Chebyshev (Chessboard) Distance Formula.
     *
     * @param radius distance in blocks to check (sphere)
     * @return list of nearby objects
     */
    public List<T> collectNearbyChebyshev(int x, int y, int z, int radius) {
        return collectNearbyWithMetric(x, y, z, radius, DistanceFormula.Chebyshev);
    }

    /**
     * Search the grid for nearby objects using Manhattan (Taxicab) Distance Formula.
     *
     * @param radius distance in blocks to check (sphere)
     * @return list of nearby objects
     */
    public List<T> collectNearbyManhattan(int x, int y, int z, int radius) {
        return collectNearbyWithMetric(x, y, z, radius, DistanceFormula.Manhattan);
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
        return findFirstNearbyWithMetric(x, y, z, radius, DistanceFormula.SquaredEuclidean);
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
        return findFirstNearbyWithMetric(x, y, z, radius, DistanceFormula.Chebyshev);
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
        return findFirstNearbyWithMetric(x, y, z, radius, DistanceFormula.Manhattan);
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
    public T findClosestSquaredEuclidean(int x, int y, int z, int radius) {
        return findClosestWithMetric(x, y, z, radius, DistanceFormula.SquaredEuclidean);
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
    public T findClosestChebyshev(int x, int y, int z, int radius) {
        return findClosestWithMetric(x, y, z, radius, DistanceFormula.Chebyshev);
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
    public T findClosestManhattan(int x, int y, int z, int radius) {
        return findClosestWithMetric(x, y, z, radius, DistanceFormula.Manhattan);
    }

    /**
     * Find the first nearby object using a specified distance metric. <br>
     * <strong>Note:</strong> This method does <strong>not</strong> return the closest object
     *
     * @param x              position to check
     * @param y              position to check
     * @param z              position to check
     * @param radius         distance in blocks to check (sphere)
     * @param distanceMetric distance metric to use
     * @return the first nearby object or null if none found
     * @see #findClosestWithMetric(int, int, int, int, DistanceMetric)
     */
    public T findFirstNearbyWithMetric(int x, int y, int z, int radius, DistanceMetric distanceMetric) {
        Iterator<T> iterator = iterNearbyWithMetric(x, y, z, radius, distanceMetric);
        return (iterator.hasNext() ? iterator.next() : null);
    }

    /**
     * Find the closest nearby object using a specified distance metric.
     *
     * @param x              position to check
     * @param y              position to check
     * @param z              position to check
     * @param radius         distance in blocks to check (sphere)
     * @param distanceMetric distance metric to use
     * @return the closest nearby object or null if none found
     */
    public T findClosestWithMetric(int x, int y, int z, int radius, DistanceMetric distanceMetric) {
        Iterator<T> iterator = iterNearbyWithMetric(x, y, z, radius, distanceMetric);
        T closestObject = null;
        double closestDistance = Double.MAX_VALUE;

        while (iterator.hasNext()) {
            T obj = iterator.next();
            positionExtractor.accept(scratch, obj);
            double distance = distanceMetric.distance(x, y, z, scratch.x, scratch.y, scratch.z);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestObject = obj;
            }
        }

        return closestObject;
    }

    /**
     * Collect all nearby objects using a specified distance metric.
     *
     * @param x              position to check
     * @param y              position to check
     * @param z              position to check
     * @param radius         distance in blocks to check (sphere)
     * @param distanceMetric distance metric to use
     * @return list of nearby objects
     */
    public List<T> collectNearbyWithMetric(int x, int y, int z, int radius, DistanceMetric distanceMetric) {
        Iterator<T> iterator = iterNearbyWithMetric(x, y, z, radius, distanceMetric);
        List<T> resultList = new ArrayList<>();

        while (iterator.hasNext()) {
            T obj = iterator.next();
            resultList.add(obj);
        }

        return resultList;
    }

    /**
     * Search the grid for nearby objects using a specified distance metric.
     *
     * @param x              position to check
     * @param y              position to check
     * @param z              position to check
     * @param radius         distance in blocks to check (sphere)
     * @param distanceMetric distance metric to use
     * @return iterator of nearby objects
     */
    public Iterator<T> iterNearbyWithMetric(int x, int y, int z, int radius, DistanceMetric distanceMetric) {
        radius = Math.abs(radius); // just no
        final int cellX = Math.floorDiv(x, cellSize);
        final int cellY = Math.floorDiv(y, cellSize);
        final int cellZ = Math.floorDiv(z, cellSize);

        return new GridIterator<>(grid, x, y, z, radius, cellSize, distanceMetric, positionExtractor);
    }

    public static class GridIterator<T> implements Iterator<T> {

        private final Long2ObjectOpenHashMap<ObjectArrayList<T>> grid;
        private final int x, y, z;
        private final int cellSize;
        private final int minCellX, minCellY, minCellZ;
        private final int maxCellX, maxCellY, maxCellZ;
        private final double distanceCompared;
        private final DistanceMetric distanceMetric;
        private final BiConsumer<Vector3i, T> positionExtractor;
        private final Vector3i scratch = new Vector3i();

        private int currCellX, currCellY, currCellZ;
        private boolean isEdgeCell;
        private ObjectArrayList<T> currentCell;
        private int currCellIdx;

        private T nextElement;
        private boolean hasNextElement;

        public GridIterator(Long2ObjectOpenHashMap<ObjectArrayList<T>> grid, int x, int y, int z, int radius,
                int cellSize, DistanceMetric distanceMetric, BiConsumer<Vector3i, T> positionExtractor) {
            this.grid = grid;
            this.x = x;
            this.y = y;
            this.z = z;
            this.cellSize = cellSize;
            this.distanceCompared = distanceMetric.transformCompareDistance(radius);
            this.distanceMetric = distanceMetric;
            this.positionExtractor = positionExtractor;

            this.minCellX = Math.floorDiv(x - radius, cellSize);
            this.minCellY = Math.floorDiv(y - radius, cellSize);
            this.minCellZ = Math.floorDiv(z - radius, cellSize);
            this.maxCellX = Math.floorDiv(x + radius + cellSize - 1, cellSize);
            this.maxCellY = Math.floorDiv(y + radius + cellSize - 1, cellSize);
            this.maxCellZ = Math.floorDiv(z + radius + cellSize - 1, cellSize);
            this.currCellX = minCellX;
            this.currCellY = minCellY;
            this.currCellZ = minCellZ;

            computeNext();
        }

        @Override
        public boolean hasNext() {
            return hasNextElement;
        }

        @Override
        public T next() {
            if (!hasNextElement) throw new NoSuchElementException();
            T result = nextElement;
            computeNext();
            return result;
        }

        public T peek() {
            if (!hasNextElement) throw new NoSuchElementException();
            return nextElement;
        }

        private void computeNext() {
            hasNextElement = false;
            nextElement = null;

            do {
                if (findNextElementInCell()) return;
            } while (findNextCell());
        }

        private boolean isCellPotentiallyInRange(int cellX, int cellY, int cellZ) {
            int nx = nearest(x, cellX, cellX + cellSize - 1);
            int ny = nearest(y, cellY, cellY + cellSize - 1);
            int nz = nearest(z, cellZ, cellZ + cellSize - 1);

            return distanceMetric.distance(x, y, z, nx, ny, nz) <= distanceCompared;
        }

        private boolean isCellFullyInRange(int cellX, int cellY, int cellZ) {
            int fx = farthest(x, cellX, cellX + cellSize - 1);
            int fy = farthest(y, cellY, cellY + cellSize - 1);
            int fz = farthest(z, cellZ, cellZ + cellSize - 1);

            return distanceMetric.distance(x, y, z, fx, fy, fz) <= distanceCompared;
        }

        private static int farthest(int p, int min, int max) {
            if (p < min) return max;
            if (p > max) return min;
            return ((p - min) > (max - p)) ? min : max;
        }

        private static int nearest(int v, int min, int max) {
            if (v < min) return min;
            return Math.min(v, max);
        }

        private boolean findNextCell() {
            while (currCellX <= maxCellX) {
                while (currCellY <= maxCellY) {
                    while (currCellZ <= maxCellZ) {
                        int currCellZ = this.currCellZ++;

                        int cx = currCellX * cellSize;
                        int cy = currCellY * cellSize;
                        int cz = currCellZ * cellSize;

                        boolean isCellInRange = isCellPotentiallyInRange(cx, cy, cz);
                        if (!isCellInRange) continue;

                        long key = pack(currCellX, currCellY, currCellZ);
                        ObjectArrayList<T> cell = grid.get(key);
                        if (cell != null && !cell.isEmpty()) {
                            currentCell = cell;
                            isEdgeCell = !isCellFullyInRange(cx, cy, cz);
                            currCellIdx = 0;
                            return true;
                        }
                    }
                    currCellZ = minCellZ;
                    currCellY++;
                }
                currCellY = minCellY;
                currCellX++;
            }
            return false;
        }

        private boolean findNextElementInCell() {
            if (currentCell == null) return false;
            while (currCellIdx < currentCell.size()) {
                T candidate = currentCell.get(currCellIdx++);
                if (!isEdgeCell || isEdgeElementInRange(candidate)) {
                    nextElement = candidate;
                    hasNextElement = true;
                    return true;
                }
            }
            currentCell = null;
            return false;
        }

        private boolean isEdgeElementInRange(T candidate) {
            positionExtractor.accept(scratch, candidate);
            double dist = distanceMetric.distance(x, y, z, scratch.x, scratch.y, scratch.z);
            return dist <= distanceCompared;
        }
    }
}
