package com.gtnewhorizon.gtnhlib.datastructs.space;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * This is a data structure that allows you to quickly check if an arbitrary point in space (dim, x, y, z) is contained
 * within at least one of the volumes defined. You can also retrieve the closest stored object from that point if any.
 * <p>
 * When adding an object to an existing position, it won't add a second object but will instead replace the element at
 * this position and update the radius.
 */
@SuppressWarnings("unused")
public class ArrayProximityMap4D<T> {

    // optimization idea : this can be dynamically
    // replaced with a map if the amount of dims gets too big
    // (I doubt the amount of dimensions will ever be too big)
    private final List<DimensionData<T>> dimList = new ArrayList<>();
    private final VolumeShape shape;

    public ArrayProximityMap4D(@Nonnull VolumeShape shape) {
        Objects.requireNonNull(shape);
        this.shape = shape;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private DimensionData<T> getDataForDim(int dim) {
        final int size = dimList.size();
        for (int i = 0; i < size; i++) {
            final DimensionData<T> dimData = dimList.get(i);
            if (dimData.getDimId() == dim) {
                return dimData;
            }
        }
        return null;
    }

    public void put(T obj, int dim, int x, int y, int z, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius must be strictly positive");
        }
        DimensionData<T> dimData = getDataForDim(dim);
        if (dimData == null) {
            dimData = new DimensionData<>(dim);
            dimList.add(dimData);
        }
        dimData.put(obj, x, y, z, radius);
    }

    public T remove(int dim, int x, int y, int z) {
        final DimensionData<T> dimData = getDataForDim(dim);
        if (dimData == null) {
            return null;
        }
        final T obj = dimData.remove(x, y, z);
        if (dimData.isEmpty()) {
            dimList.remove(dimData);
        }
        return obj;
    }

    public boolean isInVolume(int dim, double x, double y, double z) {
        final DimensionData<T> dimData = getDataForDim(dim);
        if (dimData == null) {
            return false;
        }
        if (shape == VolumeShape.SPHERE) {
            return dimData.isInSphere(x, y, z);
        } else if (shape == VolumeShape.CUBE) {
            return dimData.isInCube(x, y, z);
        }
        throw new IllegalArgumentException("Invalid shape");
    }

    public T getClosest(int dim, double x, double y, double z) {
        final DimensionData<T> dimData = getDataForDim(dim);
        if (dimData == null) {
            return null;
        }
        if (shape == VolumeShape.SPHERE) {
            return dimData.closestSphere(x, y, z);
        } else if (shape == VolumeShape.CUBE) {
            return dimData.closestCube(x, y, z);
        }
        throw new IllegalArgumentException("Invalid shape");
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public int size() {
        int size = 0;
        final int listSize = dimList.size();
        for (int i = 0; i < listSize; i++) {
            size += dimList.get(i).size();
        }
        return size;
    }

    public boolean isEmpty() {
        return dimList.isEmpty();
    }

    public void clear() {
        dimList.clear();
    }

    private static class DimensionData<T> {

        // optimization idea : the data array can be
        // dynamically replaced with a different
        // data structure if the size gets too big

        // implementation note : since we want fast
        // isInVolume() checks, when removing volumes,
        // we move the last element to the removed index
        // so that all the "alive data" in the array
        // remains at the start

        // the data array contains blocks of 4 ints
        // 1 = positionX, 2 = positionY, 3 = positionZ, 4 = radius

        private final int dimId;
        private int[] data;
        private Object[] objects;
        private int size;

        private static final int INITIAL_CAPACITY = 8;

        public DimensionData(int dimensionId) {
            this.dimId = dimensionId;
            this.data = new int[INITIAL_CAPACITY * 4];
            this.objects = new Object[INITIAL_CAPACITY];
            this.size = 0;
        }

        public int getDimId() {
            return dimId;
        }

        public int size() {
            return size;
        }

        public void put(T obj, int x, int y, int z, int radius) {
            final int maxIndex = size * 4;
            final int[] a = data;
            for (int i = 0; i < maxIndex; i += 4) {
                if (x == a[i] && y == a[i + 1] && z == a[i + 2]) {
                    a[i + 3] = radius;
                    objects[i / 4] = obj;
                    return;
                }
            }
            if (maxIndex == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
                objects = Arrays.copyOf(objects, objects.length * 2);
            }
            data[maxIndex] = x;
            data[maxIndex + 1] = y;
            data[maxIndex + 2] = z;
            data[maxIndex + 3] = radius;
            objects[size] = obj;
            size++;
        }

        @SuppressWarnings("unchecked")
        public T remove(int x, int y, int z) {
            final int maxIndex = size * 4;
            final int[] a = data;
            for (int i = 0; i < maxIndex; i += 4) {
                if (x == a[i] && y == a[i + 1] && z == a[i + 2]) {
                    Object removed = objects[i / 4];
                    int numMoved = maxIndex - i - 4;
                    if (numMoved > 0) {
                        // if it's not the last element that got removed,
                        // move the last element to the removed index
                        // to keep all data packed at the start of the array
                        System.arraycopy(data, maxIndex - 4, data, i, 4);
                        objects[i / 4] = objects[size];
                        objects[size] = null;
                    }
                    size--;
                    if (data.length >= INITIAL_CAPACITY * 4 * 2 && size * 4 < data.length / 4) {
                        data = Arrays.copyOf(data, data.length / 2);
                        objects = Arrays.copyOf(objects, objects.length / 2);
                    }
                    return (T) removed;
                }
            }
            return null;
        }

        public boolean isInSphere(double x, double y, double z) {
            final int maxIndex = size * 4;
            final int[] a = data;
            for (int i = 0; i < maxIndex; i += 4) {
                final double dx = x - a[i] - 0.5D;
                final double dy = y - a[i + 1] - 0.5D;
                final double dz = z - a[i + 2] - 0.5D;
                final double radius = a[i + 3];
                if (dx * dx + dy * dy + dz * dz < radius * radius) {
                    return true;
                }
            }
            return false;
        }

        public boolean isInCube(double x, double y, double z) {
            final int maxIndex = size * 4;
            final int[] a = data;
            for (int i = 0; i < maxIndex; i += 4) {
                final double centerX = a[i] + 0.5D;
                final double centerY = a[i + 1] + 0.5D;
                final double centerZ = a[i + 2] + 0.5D;
                final double radius = a[i + 3] + 0.5D;
                if (centerX - radius < x && x < centerX + radius
                        && centerY - radius < y
                        && y < centerY + radius
                        && centerZ - radius < z
                        && z < centerZ + radius) {
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        public T closestSphere(double x, double y, double z) {
            final int maxIndex = size * 4;
            final int[] a = data;
            int closestIndex = -1;
            double closestDistSq = Double.MAX_VALUE;
            for (int i = 0; i < maxIndex; i += 4) {
                final double dx = x - a[i] - 0.5D;
                final double dy = y - a[i + 1] - 0.5D;
                final double dz = z - a[i + 2] - 0.5D;
                final double radius = a[i + 3];
                final double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < radius * radius && distSq < closestDistSq) {
                    closestIndex = i;
                    closestDistSq = distSq;
                }
            }
            if (closestIndex == -1) return null;
            return (T) objects[closestIndex / 4];
        }

        @SuppressWarnings("unchecked")
        public T closestCube(double x, double y, double z) {
            final int maxIndex = size * 4;
            final int[] a = data;
            int closestIndex = -1;
            double closestDistSq = Double.MAX_VALUE;
            for (int i = 0; i < maxIndex; i += 4) {
                final double centerX = a[i] + 0.5D;
                final double centerY = a[i + 1] + 0.5D;
                final double centerZ = a[i + 2] + 0.5D;
                final double radius = a[i + 3] + 0.5D;
                if (centerX - radius < x && x < centerX + radius
                        && centerY - radius < y
                        && y < centerY + radius
                        && centerZ - radius < z
                        && z < centerZ + radius) {
                    final double dx = x - centerX;
                    final double dy = y - centerY;
                    final double dz = z - centerZ;
                    final double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq < radius * radius && distSq < closestDistSq) {
                        closestIndex = i;
                        closestDistSq = distSq;
                    }
                }
            }
            if (closestIndex == -1) return null;
            return (T) objects[closestIndex / 4];
        }

        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public String toString() {
            // spotless:off
            final int maxIndex = size * 4;
            final int[] a = data;
            final StringBuilder sb = new StringBuilder("DimensionData{");
            sb.append("dimId=").append(dimId);
            sb.append(", size=").append(size);
            sb.append(", data={");
            for (int i = 0; i < maxIndex; i += 4) {
                if (i != 0) sb.append(", ");
                sb.append("{x=").append(a[i]);
                sb.append(", y=").append(a[i + 1]);
                sb.append(", z=").append(a[i + 2]);
                sb.append(", radius=").append(a[i + 3]);
                sb.append(" , obj=").append(objects[i / 4]).append('}');
            }
            sb.append("}}");
            return sb.toString();
            // spotless:on
        }
    }

}
