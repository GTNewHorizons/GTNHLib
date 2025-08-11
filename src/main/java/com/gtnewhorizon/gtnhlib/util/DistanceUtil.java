package com.gtnewhorizon.gtnhlib.util;

public class DistanceUtil {

    public static double squaredEuclideanDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * aka Taxicab distance
     */
    public static double manhattanDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1) + Math.abs(z2 - z1);
    }

    /**
     * aka Chessboard distance
     */
    public static double chebyshevDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
    }
}
