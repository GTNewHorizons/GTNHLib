package com.gtnewhorizon.gtnhlib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;

/// Boxes within a block (0 to 1 coordinates), outlined as one merged shape. Build each shape once and reuse it.
public final class OutlineShape {

    /// No shape: the block's own outline and ray trace are used.
    public static final OutlineShape EMPTY = new OutlineShape(Collections.emptyList());

    private static final double TOLERANCE = 1.0E-6;

    private static final int MAX_CELLS = 32 * 32 * 32;

    private final List<AxisAlignedBB> boxes;
    private DoubleList lines;

    private OutlineShape(List<AxisAlignedBB> boxes) {
        this.boxes = boxes;
    }

    public static OutlineShape of(AxisAlignedBB... boxes) {
        return of(Arrays.asList(boxes));
    }

    /// Boxes with no thickness are ignored.
    public static OutlineShape of(List<AxisAlignedBB> boxes) {
        return of(boxes, 0, 0, 0);
    }

    public static OutlineShape fromCollision(Block block, World world, int x, int y, int z) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        AxisAlignedBB mask = AxisAlignedBB.getBoundingBox(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2);
        block.addCollisionBoxesToList(world, x, y, z, mask, boxes, null);
        return of(boxes, -x, -y, -z);
    }

    private static OutlineShape of(List<AxisAlignedBB> boxes, double dx, double dy, double dz) {
        List<AxisAlignedBB> solid = new ArrayList<>(boxes.size());
        for (AxisAlignedBB box : boxes) {
            if (isSolid(box)) solid.add(box.getOffsetBoundingBox(dx, dy, dz));
        }
        return solid.isEmpty() ? EMPTY : new OutlineShape(Collections.unmodifiableList(solid));
    }

    private static boolean isSolid(AxisAlignedBB box) {
        for (int axis = 0; axis < 3; axis++) {
            if (max(box, axis) - min(box, axis) <= TOLERANCE) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    public boolean hasSameBoxes(OutlineShape other) {
        if (other == this) return true;
        if (other.boxes.size() != boxes.size()) return false;
        for (int n = 0; n < boxes.size(); n++) {
            for (int axis = 0; axis < 3; axis++) {
                if (min(boxes.get(n), axis) != min(other.boxes.get(n), axis)
                        || max(boxes.get(n), axis) != max(other.boxes.get(n), axis)) {
                    return false;
                }
            }
        }
        return true;
    }

    public MovingObjectPosition rayTrace(int x, int y, int z, Vec3 start, Vec3 end) {
        MovingObjectPosition closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (AxisAlignedBB box : boxes) {
            MovingObjectPosition hit = box.getOffsetBoundingBox(x, y, z).calculateIntercept(start, end);
            if (hit == null) continue;
            double distance = start.squareDistanceTo(hit.hitVec);
            if (distance < closestDistance) {
                closest = hit;
                closestDistance = distance;
            }
        }
        return closest == null ? null : new MovingObjectPosition(x, y, z, closest.sideHit, closest.hitVec);
    }

    /// The outline's lines, flattened to `x1, y1, z1, x2, y2, z2` each. Empty if the boxes make a grid of over 32³
    /// cells,
    /// which is too costly to merge.
    public DoubleList getLines() {
        if (lines == null) lines = isEmpty() ? DoubleLists.EMPTY_LIST : buildLines();
        return lines;
    }

    /// Splits the boxes into a grid of cells, then draws each grid line whose four cells aren't alike or a flat face.
    private DoubleList buildLines() {
        double[][] grid = { gridLines(0), gridLines(1), gridLines(2) };
        if ((long) (grid[0].length - 1) * (grid[1].length - 1) * (grid[2].length - 1) > MAX_CELLS) {
            return DoubleLists.EMPTY_LIST;
        }
        boolean[][][] filled = new boolean[grid[0].length - 1][grid[1].length - 1][grid[2].length - 1];
        for (AxisAlignedBB box : boxes) {
            int[] x = cellRange(grid[0], min(box, 0), max(box, 0));
            int[] y = cellRange(grid[1], min(box, 1), max(box, 1));
            int[] z = cellRange(grid[2], min(box, 2), max(box, 2));
            for (int i = x[0]; i < x[1]; i++) {
                for (int j = y[0]; j < y[1]; j++) {
                    for (int k = z[0]; k < z[1]; k++) {
                        filled[i][j][k] = true;
                    }
                }
            }
        }

        DoubleArrayList segments = new DoubleArrayList();
        for (int axis = 0; axis < 3; axis++) {
            double[] along = grid[axis], us = grid[(axis + 1) % 3], vs = grid[(axis + 2) % 3];
            for (int u = 0; u < us.length; u++) {
                for (int v = 0; v < vs.length; v++) {
                    int start = -1;
                    for (int i = 0; i < along.length; i++) {
                        boolean edge = i < along.length - 1 && isEdge(
                                isFilled(filled, axis, i, u - 1, v - 1),
                                isFilled(filled, axis, i, u, v - 1),
                                isFilled(filled, axis, i, u - 1, v),
                                isFilled(filled, axis, i, u, v));
                        if (edge && start == -1) start = i;
                        if (!edge && start != -1) {
                            addPoint(segments, axis, along[start], us[u], vs[v]);
                            addPoint(segments, axis, along[i], us[u], vs[v]);
                            start = -1;
                        }
                    }
                }
            }
        }
        return DoubleLists.unmodifiable(segments);
    }

    private double[] gridLines(int axis) {
        double[] coords = new double[boxes.size() * 2];
        for (int n = 0; n < boxes.size(); n++) {
            coords[n * 2] = min(boxes.get(n), axis);
            coords[n * 2 + 1] = max(boxes.get(n), axis);
        }
        Arrays.sort(coords);

        int count = 0;
        for (double coord : coords) {
            if (count == 0 || coord - coords[count - 1] > TOLERANCE) coords[count++] = coord;
        }
        return Arrays.copyOf(coords, count);
    }

    private static int[] cellRange(double[] coords, double min, double max) {
        int from = 0;
        while (from < coords.length - 1 && (coords[from] + coords[from + 1]) / 2 < min) from++;
        int to = from;
        while (to < coords.length - 1 && (coords[to] + coords[to + 1]) / 2 < max) to++;
        return new int[] { from, to };
    }

    private static boolean isFilled(boolean[][][] filled, int axis, int i, int u, int v) {
        int x = axis == 0 ? i : axis == 1 ? v : u;
        int y = axis == 0 ? u : axis == 1 ? i : v;
        int z = axis == 0 ? v : axis == 1 ? u : i;
        return x >= 0 && y >= 0
                && z >= 0
                && x < filled.length
                && y < filled[x].length
                && z < filled[x][y].length
                && filled[x][y][z];
    }

    /// The cells around a grid line, `a`/`d` and `b`/`c` diagonal. Not an edge if they pair up (flat or no change).
    private static boolean isEdge(boolean a, boolean b, boolean c, boolean d) {
        return !(a == b && c == d) && !(a == c && b == d);
    }

    private static void addPoint(DoubleArrayList segments, int axis, double along, double u, double v) {
        segments.add(axis == 0 ? along : axis == 1 ? v : u);
        segments.add(axis == 0 ? u : axis == 1 ? along : v);
        segments.add(axis == 0 ? v : axis == 1 ? u : along);
    }

    private static double min(AxisAlignedBB box, int axis) {
        return axis == 0 ? box.minX : axis == 1 ? box.minY : box.minZ;
    }

    private static double max(AxisAlignedBB box, int axis) {
        return axis == 0 ? box.maxX : axis == 1 ? box.maxY : box.maxZ;
    }
}
