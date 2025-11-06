package com.gtnewhorizon.gtnhlib.visualization;

import java.awt.*;

import org.joml.primitives.AABBf;
import org.joml.primitives.AABBfc;

public class VisualizedBox {

    public final Color color;
    public AABBfc bounds;

    public VisualizedBox(Color color, AABBfc bounds) {
        this.color = color;
        this.bounds = bounds;
    }

    public VisualizedBox(int rgba, AABBfc bounds) {
        this.color = new Color((rgba >> 24) & 0xFF, (rgba >> 16) & 0xFF, (rgba >> 8) & 0xFF, rgba & 0xFF);
        this.bounds = bounds;
    }

    public VisualizedBox expand(float amount) {
        bounds = new AABBf(
            bounds.minX() - amount, bounds.minY() - amount, bounds.minZ() - amount,
            bounds.maxX() + amount, bounds.maxY() + amount, bounds.maxZ() + amount);

        return this;
    }

    @Override
    public String toString() {
        return "VisualizedBox{" + "color=" + color + ", bounds=" + bounds + '}';
    }
}
