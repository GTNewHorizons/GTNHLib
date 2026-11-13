package com.gtnewhorizon.gtnhlib.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import com.github.bsideup.jabel.Desugar;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;

/// Sloped faces of an [OutlineShape].
/// Along each edge, faces cancel in pairs (flat seams or back to back). Edges with a face left over are drawn.
final class FaceShape {

    private static final double SNAP = 4096, TOLERANCE = 1.0E-6;

    @Desugar
    private record LineKey(long dx, long dy, long dz, long fx, long fy, long fz) {}

    @Desugar
    private record Edge(double from, double to, double[] normal, double[] inward) {}

    /// `foot` is the line's point nearest the origin.
    @Desugar
    private record Line(double[] dir, double[] foot, List<Edge> edges) {}

    private final List<double[]> faces = new ArrayList<>();
    private final List<double[]> normals = new ArrayList<>();

    FaceShape(List<double[]> faces) {
        for (double[] face : faces) {
            double[] snapped = new double[face.length - face.length % 3];
            for (int i = 0; i < snapped.length; i++) snapped[i] = snap(face[i]);
            double[] normal = normal(snapped);
            if (normal == null) continue;
            this.faces.add(snapped);
            this.normals.add(normal);
        }
    }

    boolean isEmpty() {
        return faces.isEmpty();
    }

    /// The nearest face entered. The side is its normal's main axis, up on a tie.
    MovingObjectPosition rayTrace(int x, int y, int z, Vec3 start, Vec3 end) {
        double sx = start.xCoord - x, sy = start.yCoord - y, sz = start.zCoord - z;
        double dx = end.xCoord - start.xCoord, dy = end.yCoord - start.yCoord, dz = end.zCoord - start.zCoord;
        double best = Double.MAX_VALUE;
        int bestFace = -1;
        for (int f = 0; f < faces.size(); f++) {
            double t = hit(f, sx, sy, sz, dx, dy, dz);
            if (t >= 0 && t < best) {
                best = t;
                bestFace = f;
            }
        }
        if (bestFace == -1) return null;
        double[] n = normals.get(bestFace);
        double ax = Math.abs(n[0]), ay = Math.abs(n[1]), az = Math.abs(n[2]);
        int side = ay >= ax - TOLERANCE && ay >= az - TOLERANCE ? (n[1] > 0 ? 1 : 0)
                : az >= ax - TOLERANCE ? (n[2] > 0 ? 3 : 2) : (n[0] > 0 ? 5 : 4);
        Vec3 hit = Vec3
                .createVectorHelper(start.xCoord + best * dx, start.yCoord + best * dy, start.zCoord + best * dz);
        return new MovingObjectPosition(x, y, z, side, hit);
    }

    /// How far along the ray (0 to 1) it enters face `f` from the front, or -1.
    private double hit(int f, double sx, double sy, double sz, double dx, double dy, double dz) {
        double[] face = faces.get(f), n = normals.get(f);
        double facing = n[0] * dx + n[1] * dy + n[2] * dz;
        if (facing > -TOLERANCE) return -1;
        double t = (n[0] * (face[0] - sx) + n[1] * (face[1] - sy) + n[2] * (face[2] - sz)) / facing;
        if (t < 0 || t > 1) return -1;
        return contains(face, n, sx + t * dx, sy + t * dy, sz + t * dz) ? t : -1;
    }

    /// Whether a point on the face's plane is within it, border included.
    private static boolean contains(double[] face, double[] n, double px, double py, double pz) {
        int count = face.length / 3;
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count;
            double ex = face[j * 3] - face[i * 3], ey = face[j * 3 + 1] - face[i * 3 + 1],
                    ez = face[j * 3 + 2] - face[i * 3 + 2];
            double qx = px - face[i * 3], qy = py - face[i * 3 + 1], qz = pz - face[i * 3 + 2];
            double side = n[0] * (ey * qz - ez * qy) + n[1] * (ez * qx - ex * qz) + n[2] * (ex * qy - ey * qx);
            if (side < -TOLERANCE) return false;
        }
        return true;
    }

    DoubleList buildLines() {
        Map<LineKey, Line> lines = new HashMap<>();
        for (int f = 0; f < faces.size(); f++) addFace(lines, faces.get(f), normals.get(f));
        DoubleArrayList segments = new DoubleArrayList();
        for (Line line : lines.values()) addLine(segments, line);
        return DoubleLists.unmodifiable(segments);
    }

    private static void addFace(Map<LineKey, Line> lines, double[] face, double[] normal) {
        int count = face.length / 3;
        double[] centre = new double[3];
        for (int i = 0; i < count; i++) {
            for (int a = 0; a < 3; a++) centre[a] += face[i * 3 + a] / count;
        }
        for (int i = 0; i < count; i++) {
            double[] p = vertex(face, i), q = vertex(face, (i + 1) % count);
            double[] dir = sub(q, p);
            double length = Math.sqrt(dot(dir, dir));
            if (length < TOLERANCE) continue;
            for (int a = 0; a < 3; a++) dir[a] /= length;
            if (!isCanonical(dir)) negate(dir);
            double[] inward = cross(normal, dir);
            if (dot(inward, sub(centre, p)) < 0) negate(inward);
            double from = dot(p, dir), to = dot(q, dir);
            double[] foot = { p[0] - from * dir[0], p[1] - from * dir[1], p[2] - from * dir[2] };
            LineKey key = new LineKey(key(dir[0]), key(dir[1]), key(dir[2]), key(foot[0]), key(foot[1]), key(foot[2]));
            lines.computeIfAbsent(key, k -> new Line(dir, foot, new ArrayList<>())).edges()
                    .add(new Edge(Math.min(from, to), Math.max(from, to), normal, inward));
        }
    }

    /// Draws the parts of the line where faces don't cancel.
    private static void addLine(DoubleArrayList segments, Line line) {
        List<Edge> edges = line.edges();
        double[] cuts = new double[edges.size() * 2];
        for (int i = 0; i < edges.size(); i++) {
            cuts[i * 2] = edges.get(i).from();
            cuts[i * 2 + 1] = edges.get(i).to();
        }
        Arrays.sort(cuts);
        int count = 0;
        for (double cut : cuts) {
            if (count == 0 || cut - cuts[count - 1] > TOLERANCE) cuts[count++] = cut;
        }

        int start = -1;
        for (int i = 0; i < count; i++) {
            boolean bends = i < count - 1 && bends(edges, (cuts[i] + cuts[i + 1]) / 2);
            if (bends && start == -1) start = i;
            if (!bends && start != -1) {
                addPoint(segments, line, cuts[start]);
                addPoint(segments, line, cuts[i]);
                start = -1;
            }
        }
    }

    private static boolean bends(List<Edge> edges, double at) {
        List<Edge> touching = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.from() < at && at < edge.to()) touching.add(edge);
        }
        boolean[] paired = new boolean[touching.size()];
        for (int i = 0; i < touching.size(); i++) {
            for (int j = i + 1; j < touching.size() && !paired[i]; j++) {
                if (!paired[j] && cancels(touching.get(i), touching.get(j))) paired[i] = paired[j] = true;
            }
            if (!paired[i]) return true;
        }
        return false;
    }

    private static boolean cancels(Edge a, Edge b) {
        double facing = dot(a.normal(), b.normal()), side = dot(a.inward(), b.inward());
        return facing > 1 - TOLERANCE && side < -1 + TOLERANCE || facing < -1 + TOLERANCE && side > 1 - TOLERANCE;
    }

    private static double[] normal(double[] face) {
        int count = face.length / 3;
        double[] n = new double[3];
        for (int i = 0; i < count; i++) {
            double[] p = vertex(face, i), q = vertex(face, (i + 1) % count);
            n[0] += (p[1] - q[1]) * (p[2] + q[2]);
            n[1] += (p[2] - q[2]) * (p[0] + q[0]);
            n[2] += (p[0] - q[0]) * (p[1] + q[1]);
        }
        double length = Math.sqrt(dot(n, n));
        if (length < TOLERANCE) return null;
        for (int a = 0; a < 3; a++) n[a] /= length;
        return n;
    }

    private static void addPoint(DoubleArrayList segments, Line line, double t) {
        // Ends are corners, so snapping undoes rounding.
        for (int a = 0; a < 3; a++) segments.add(snap(line.foot()[a] + t * line.dir()[a]));
    }

    private static double[] vertex(double[] face, int i) {
        return new double[] { face[i * 3], face[i * 3 + 1], face[i * 3 + 2] };
    }

    private static double snap(double value) {
        return Math.round(value * SNAP) / SNAP;
    }

    private static long key(double value) {
        return Math.round(value / TOLERANCE);
    }

    /// First non-zero component positive, so both directions along a line give one key.
    private static boolean isCanonical(double[] dir) {
        for (double c : dir) {
            if (Math.abs(c) > TOLERANCE) return c > 0;
        }
        return true;
    }

    private static void negate(double[] v) {
        for (int a = 0; a < 3; a++) v[a] = -v[a];
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }
}
