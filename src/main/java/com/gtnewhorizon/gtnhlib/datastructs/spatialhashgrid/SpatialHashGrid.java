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

        return new GridIterator<>(
                grid,
                x,
                y,
                z,
                cellSize,
                cellRad,
                cellX,
                cellY,
                cellZ,
                distanceCompared,
                distanceFormula,
                positionExtractor);
    }

    protected static double distanceBetweenPoints(double x1, double y1, double z1, double x2, double y2, double z2,
            DistanceFormula distanceFormula) {
        return switch (distanceFormula) {
            case SquaredEuclidean -> DistanceUtil.squaredEuclideanDistance(x1, y1, z1, x2, y2, z2);
            case Chebyshev -> DistanceUtil.chebyshevDistance(x1, y1, z1, x2, y2, z2);
            case Manhattan -> DistanceUtil.manhattanDistance(x1, y1, z1, x2, y2, z2);
        };
    }

    public static class GridIterator<T> implements Iterator<T> {

        private ObjectArrayList<T> currentList;
        private int currentIndex;
        private int cellXOffset;
        private int cellYOffeset;
        private int cellZOffset;

        private final Long2ObjectOpenHashMap<ObjectArrayList<T>> grid;
        private final int x;
        private final int y;
        private final int z;
        private final int cellSize;
        private final int cellRad;
        private final int cellX;
        private final int cellY;
        private final int cellZ;
        private final int distanceCompared;
        private final DistanceFormula distanceFormula;
        private final BiConsumer<Vector3i, T> positionExtractor;
        private final Vector3i scratch = new Vector3i();

        public GridIterator(Long2ObjectOpenHashMap<ObjectArrayList<T>> grid, int x, int y, int z, int cellSize,
                int cellRad, int cellX, int cellY, int cellZ, int distanceCompared, DistanceFormula distanceFormula,
                BiConsumer<Vector3i, T> positionExtractor) {
            this.grid = grid;
            this.x = x;
            this.y = y;
            this.z = z;
            this.cellSize = cellSize;
            this.cellRad = cellRad;
            cellXOffset = -cellRad;
            cellYOffeset = -cellRad;
            cellZOffset = -cellRad;
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellZ = cellZ;
            this.distanceCompared = distanceCompared;
            this.distanceFormula = distanceFormula;
            this.positionExtractor = positionExtractor;
            advance();
        }

        private void advance() {
            currentIndex = 0;
            currentList = null;

            while (cellXOffset <= cellRad) {
                while (cellYOffeset <= cellRad) {
                    while (cellZOffset <= cellRad) {
                        long key = pack(cellX + cellXOffset, cellY + cellYOffeset, cellZ + cellZOffset);

                        var originalList = grid.get(key);
                        if (originalList == null) {
                            cellZOffset++;
                            continue;
                        }

                        boolean isEdge = (Math.abs(cellXOffset) == cellRad) || (Math.abs(cellYOffeset) == cellRad)
                                || (Math.abs(cellZOffset) == cellRad);
                        var newList = new ObjectArrayList<T>();
                        if (isEdge || distanceCompared <= cellSize) {
                            for (T obj : originalList) {
                                positionExtractor.accept(scratch, obj);
                                if (distanceBetweenPoints(x, y, z, scratch.x, scratch.y, scratch.z, distanceFormula)
                                        <= distanceCompared) {
                                    newList.add(obj);
                                }
                            }
                        } else {
                            newList.addAll(originalList);
                        }

                        cellZOffset++;

                        if (!newList.isEmpty()) {
                            currentList = newList;
                            return;
                        }
                    }
                    cellZOffset = -cellRad;
                    cellYOffeset++;
                }
                cellYOffeset = -cellRad;
                cellXOffset++;
            }
        }

        @Override
        public boolean hasNext() {
            return currentList != null && currentIndex < currentList.size();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            T obj = currentList.get(currentIndex);
            currentIndex++;
            if (currentIndex >= currentList.size()) {
                advance();
            }

            return obj;
        }

        public T peek() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return currentList.get(currentIndex);
        }
    }
}
