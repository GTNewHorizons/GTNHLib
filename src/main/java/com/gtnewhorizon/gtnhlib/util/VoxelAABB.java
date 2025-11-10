package com.gtnewhorizon.gtnhlib.util;

import java.text.MessageFormat;
import java.util.Iterator;

import javax.annotation.Nonnull;

import net.minecraft.util.AxisAlignedBB;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.primitives.AABBf;

import com.google.common.collect.AbstractIterator;
import com.gtnewhorizon.gtnhlib.util.dynamicaabbtree.Boundable;

@SuppressWarnings("unused")
public class VoxelAABB implements Iterable<Vector3ic>, Boundable {

    public Vector3i origin, a, b;

    public VoxelAABB() {
        origin = new Vector3i();
        a = new Vector3i();
        b = new Vector3i();
    }

    public VoxelAABB(Vector3i a, Vector3i b) {
        this.origin = new Vector3i(a);
        this.a = new Vector3i(a);
        this.b = new Vector3i(b);
    }

    public Vector3i min() {
        return new Vector3i(a).min(b);
    }

    public Vector3i max() {
        return new Vector3i(a).max(b);
    }

    public VoxelAABB union(Vector3i v) {
        Vector3i min = min(), max = max();

        a.set(v)
            .min(min);
        b.set(v)
            .max(max);

        return this;
    }

    public VoxelAABB union(VoxelAABB other) {
        Vector3i min = min(), max = max();

        a.set(min)
            .min(other.min());
        b.set(max)
            .max(other.max());

        return this;
    }

    public VoxelAABB moveOrigin(Vector3i newOrigin) {
        b.sub(origin)
            .add(newOrigin);
        a.sub(origin)
            .add(newOrigin);
        origin.set(newOrigin);

        return this;
    }

    public VoxelAABB moveOrigin(int dx, int dy, int dz) {
        a.add(dx, dy, dz);
        b.add(dx, dy, dz);
        origin.add(dx, dy, dz);

        return this;
    }

    public VoxelAABB scale(int x, int y, int z) {
        int dirX = b.x < a.x ? -1 : 1;
        int dirY = b.y < a.y ? -1 : 1;
        int dirZ = b.z < a.z ? -1 : 1;

        Vector3i size = size();

        size.mul(x, y, z);
        size.mul(dirX, dirY, dirZ);
        size.add(origin);

        VoxelAABB other = clone();

        other.moveOrigin(size);

        union(other);

        return this;
    }

    public boolean contains(int x, int y, int z) {
        return x >= Math.min(a.x, b.x) && x <= Math.max(a.x, b.x)
            && y >= Math.min(a.y, b.y) && y <= Math.max(a.y, b.y)
            && z >= Math.min(a.z, b.z) && z <= Math.max(a.z, b.z);
    }

    public boolean contains(VoxelAABB other) {
        final Vector3i a1 = a;
        final Vector3i b1 = b;
        final int xmin1 = Math.min(a1.x, b1.x);
        final int ymin1 = Math.min(a1.y, b1.y);
        final int zmin1 = Math.min(a1.z, b1.z);
        final int xmax1 = Math.max(a1.x, b1.x);
        final int ymax1 = Math.max(a1.y, b1.y);
        final int zmax1 = Math.max(a1.z, b1.z);

        final Vector3i a2 = other.a;
        final Vector3i b2 = other.b;
        final int xmin2 = Math.min(a2.x, b2.x);
        final int ymin2 = Math.min(a2.y, b2.y);
        final int zmin2 = Math.min(a2.z, b2.z);
        final int xmax2 = Math.max(a2.x, b2.x);
        final int ymax2 = Math.max(a2.y, b2.y);
        final int zmax2 = Math.max(a2.z, b2.z);

        return xmax2 > xmin1
            && xmin2 < xmax1
            && ymax2 > ymin1
            && ymin2 < ymax1
            && zmax2 > zmin1
            && zmin2 < zmax1;
    }

    @Override
    public @Nonnull Iterator<Vector3ic> iterator() {
        Vector3i min = min();
        Vector3i max = max();

        return new AbstractIterator<>() {

            private final Vector3i v = new Vector3i();

            private int x = min.x;
            private int y = min.y;
            private int z = min.z;

            @Override
            protected Vector3ic computeNext() {
                if (x > max.x) {
                    x = min.x;
                    y++;
                }
                if (y > max.y) {
                    y = min.y;
                    z++;
                }
                if (z > max.z) {
                    this.endOfData();
                    return null;
                }
                v.set(x, y, z);
                x++;
                return v;
            }
        };
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public VoxelAABB clone() {
        VoxelAABB dup = new VoxelAABB();
        dup.origin = new Vector3i(origin);
        dup.a = new Vector3i(a);
        dup.b = new Vector3i(b);
        return dup;
    }

    public Vector3i span() {
        Vector3i min = min(), max = max();

        return new Vector3i(max.x - min.x, max.y - min.y, max.z - min.z);
    }

    public Vector3i size() {
        Vector3i min = min(), max = max();

        return new Vector3i(max.x - min.x + 1, max.y - min.y + 1, max.z - min.z + 1);
    }

    public AxisAlignedBB toBoundingBox() {
        Vector3i min = min(), max = max();

        return AxisAlignedBB.getBoundingBox(min.x, min.y, min.z, max.x + 1, max.y + 1, max.z + 1);
    }

    public AABBf getAABB() {
        return getAABB(null);
    }

    @Override
    public AABBf getAABB(AABBf dest) {
        if (dest == null) dest = new AABBf();

        Vector3i min = min(), max = max();

        dest.setMin(min.x + 0.1f, min.y + 0.1f, min.z + 0.1f);
        dest.setMax(max.x + 0.9f, max.y + 0.9f, max.z + 0.9f);

        return dest;
    }

    public String describe() {
        Vector3i size = size();

        return String.format(
            "dX=%,d dY=%,d dZ=%,d V=%,d",
            Math.abs(size.x),
            Math.abs(size.y),
            Math.abs(size.z),
            size.x * size.y * size.z);
    }

    @Override
    public String toString() {
        return MessageFormat.format("VoxelAABB'{'origin={0},{1},{2}, a={3},{4},{5}, b={6},{7},{8}'}'", origin.x, origin.y, origin.z, a.x, a.y, a.z, b.x, b.y, b.z);
    }
}
