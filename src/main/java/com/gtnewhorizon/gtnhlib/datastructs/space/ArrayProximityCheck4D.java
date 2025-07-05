package com.gtnewhorizon.gtnhlib.datastructs.space;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * This is a data structure that allows you to quickly check if an arbitrary point in space (dim, x, y, z) is contained
 * within at least one of the volumes defined.
 * <p>
 * When adding a volume to an existing position, it won't add a second volume but will instead update the radius of the
 * element at that position.
 */
@SuppressWarnings("unused")
public class ArrayProximityCheck4D {

    // optimization idea : this can be dynamically
    // replaced with a map if the amount of dims gets too big
    // (I doubt the amount of dimensions will ever be too big)
    private final List<DimensionData> dimList = new ArrayList<>();
    private final VolumeShape shape;

    public ArrayProximityCheck4D(@Nonnull VolumeShape shape) {
        Objects.requireNonNull(shape);
        this.shape = shape;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private DimensionData getDataForDim(int dim) {
        final int size = dimList.size();
        for (int i = 0; i < size; i++) {
            final DimensionData dimData = dimList.get(i);
            if (dimData.getDimId() == dim) {
                return dimData;
            }
        }
        return null;
    }

    public void put(int dim, int x, int y, int z, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius must be strictly positive");
        }
        DimensionData dimData = getDataForDim(dim);
        if (dimData == null) {
            dimData = new DimensionData(dim);
            dimList.add(dimData);
        }
        dimData.put(x, y, z, radius);
    }

    public void remove(int dim, int x, int y, int z) {
        final DimensionData dimData = getDataForDim(dim);
        if (dimData == null) {
            return;
        }
        dimData.remove(x, y, z);
        if (dimData.isEmpty()) {
            dimList.remove(dimData);
        }
    }

    public boolean isInRange(int dim, double x, double y, double z) {
        final DimensionData dimData = getDataForDim(dim);
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

    private static class DimensionData {

        // optimization idea : the data array can be
        // dynamically replaced with a different
        // data structure if the size gets too big

        // implementation note : since we want fast
        // isInRange() checks, when removing volumes,
        // we move the last element to the removed index
        // so that all the "alive data" in the array
        // remains at the start

        // the data array contains blocks of 4 ints
        // 1 = positionX, 2 = positionY, 3 = positionZ, 4 = radius

        private final int dimId;
        private int[] data;
        private int size;

        private static final int INITIAL_CAPACITY = 8;

        public DimensionData(int dimensionId) {
            this.dimId = dimensionId;
            this.data = new int[INITIAL_CAPACITY * 4];
            this.size = 0;
        }

        public int getDimId() {
            return dimId;
        }

        public int size() {
            return size;
        }

        public void put(int x, int y, int z, int radius) {
            final int maxIndex = size * 4;
            final int[] a = data;
            for (int i = 0; i < maxIndex; i += 4) {
                if (x == a[i] && y == a[i + 1] && z == a[i + 2]) {
                    a[i + 3] = radius;
                    return;
                }
            }
            if (maxIndex == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[maxIndex] = x;
            data[maxIndex + 1] = y;
            data[maxIndex + 2] = z;
            data[maxIndex + 3] = radius;
            size++;
        }

        public void remove(int x, int y, int z) {
            final int maxIndex = size * 4;
            final int[] a = data;
            for (int i = 0; i < maxIndex; i += 4) {
                if (x == a[i] && y == a[i + 1] && z == a[i + 2]) {
                    int numMoved = maxIndex - i - 4;
                    if (numMoved > 0) {
                        // if it's not the last element that got removed,
                        // move the last element to the removed index
                        // to keep all data packed at the start of the array
                        System.arraycopy(data, maxIndex - 4, data, i, 4);
                    }
                    size--;
                    if (data.length >= INITIAL_CAPACITY * 4 * 2 && size * 4 < data.length / 4) {
                        data = Arrays.copyOf(data, data.length / 2);
                    }
                    return;
                }
            }
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
                sb.append(", radius=").append(a[i + 3]).append('}');
            }
            sb.append("}}");
            return sb.toString();
            // spotless:on
        }
    }

}
