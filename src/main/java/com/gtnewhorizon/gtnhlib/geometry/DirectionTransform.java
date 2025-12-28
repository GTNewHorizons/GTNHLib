package com.gtnewhorizon.gtnhlib.geometry;

import java.util.function.Function;

import net.minecraftforge.common.util.ForgeDirection;

/// A transform that mutates ForgeDirections, represented as a pure function.
/// The transform function must map each direction onto one and only one other direction. It must never return the
/// same direction for two distinct input directions.
public interface DirectionTransform extends Function<ForgeDirection, ForgeDirection>, TransformLike {

    DirectionTransform IDENTITY = dir -> dir;

    default <T extends OneDegreeOfFreedom<T>> T transform(T value) {
        return value.withA(apply(value.getA()));
    }

    default <T extends TwoDegreesOfFreedom<T>> T transform(T value) {
        return value.withAB(apply(value.getA()), apply(value.getB()));
    }

    default <T extends ThreeDegreesOfFreedom<T>> T transform(T value) {
        return value.withABC(apply(value.getA()), apply(value.getB()), apply(value.getC()));
    }

    default DirectionTransform precalculated() {
        ForgeDirection[] transform = new ForgeDirection[6];

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            transform[dir.ordinal()] = this.apply(dir);
        }

        return dir -> transform[dir.ordinal()];
    }

    default DirectionTransform inverse() {
        ForgeDirection[] transform = new ForgeDirection[6];

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            transform[this.apply(dir).ordinal()] = dir;
        }

        return dir -> transform[dir.ordinal()];
    }

    static DirectionTransform mirror(Axis axis) {
        return axis::flip;
    }

    static DirectionTransform rotate(ForgeDirection vector, int amount) {
        return new Transform().rotate(vector, amount);
    }
}
