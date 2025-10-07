package com.gtnewhorizon.gtnhlib.client.renderer.quad;

public enum Axis {

    X,
    Y,
    Z;

    public static Axis fromName(String dir) {
        return switch (dir) {
            case "y" -> Y;
            case "z" -> Z;
            case "x" -> X;
            default -> null;
        };

    }

}
