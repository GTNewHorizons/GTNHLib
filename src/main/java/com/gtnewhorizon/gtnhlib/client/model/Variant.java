package com.gtnewhorizon.gtnhlib.client.model;

import static java.lang.Math.toRadians;

import lombok.Getter;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;

@Internal
public class Variant {

    @Getter
    private final ResourceLocation model;
    private final float x;
    private final float y;
    private final float z;
    private final boolean uvLock;
    public final int weight;

    public Variant(ResourceLocation model, int x, int y, boolean uvLock) {
        this(model, x, y, 0, uvLock, 1);
    }

    public Variant(ResourceLocation model, int x, int y, int z, boolean uvLock) {
        this(model, x, y, z, uvLock, 1);
    }

    public Variant(ResourceLocation model, int x, int y, int z, boolean uvLock, int weight) {
        this.model = model;
        this.x = (float) toRadians(x);
        this.y = (float) toRadians(y);
        this.z = (float) toRadians(z);
        this.uvLock = uvLock;
        this.weight = weight;
    }

    public Matrix4f getAffineMatrix() {

        return new Matrix4f().translation(-.5f, -.5f, -.5f).rotateLocalX(x).rotateLocalY(y).rotateLocalZ(z)
                .translateLocal(.5f, .5f, .5f);
    }
}
