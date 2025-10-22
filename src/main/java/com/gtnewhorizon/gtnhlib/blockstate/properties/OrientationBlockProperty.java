package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.TransformableProperty;
import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;
import com.gtnewhorizon.gtnhlib.geometry.Orientation;

public interface OrientationBlockProperty extends BlockProperty<Orientation>, TransformableProperty<Orientation> {

    @Override
    default String getName() {
        return "orientation";
    }

    @Override
    default Type getType() {
        return Orientation.class;
    }

    @Override
    default JsonElement serialize(Orientation value) {
        return new JsonPrimitive(stringify(value));
    }

    @Override
    default Orientation deserialize(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? parse(element.getAsString())
                : Orientation.UNKNOWN;
    }

    @Override
    default String stringify(Orientation value) {
        return value.name().toLowerCase();
    }

    @Override
    default Orientation parse(String text) throws InvalidPropertyTextException {
        try {
            return Orientation.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyTextException("Invalid Orientation", e);
        }
    }

    @Override
    default @NotNull Orientation transform(Orientation value, DirectionTransform transform) {
        ForgeDirection a = transform.apply(value.a);
        ForgeDirection b = transform.apply(value.b);

        Orientation transformed = Orientation.getOrientation(a, b);

        return isValidOrientation(transformed) ? transformed : value;
    }

    default boolean isValidOrientation(Orientation value) {
        return true;
    }

    interface O2M {

        int getMeta(Orientation dir);
    }

    interface M2O {

        Orientation getDir(int meta);
    }

    static OrientationBlockProperty orientation(int mask, O2M o2m, M2O m2o) {
        return new MetaOrientationBlockProperty(m2o, mask, o2m);
    }

    class MetaOrientationBlockProperty implements OrientationBlockProperty, MetaBlockProperty<Orientation> {

        private final M2O m2o;
        private final int mask;
        private final O2M o2m;

        public MetaOrientationBlockProperty(M2O m2o, int mask, O2M o2m) {
            this.m2o = m2o;
            this.mask = mask;
            this.o2m = o2m;
        }

        @Override
        public boolean needsExisting() {
            return mask != -1;
        }

        @Override
        public int getMeta(Orientation value, int existing) {
            return o2m.getMeta(value) | (existing & ~mask);
        }

        @Override
        public Orientation getValue(int meta) {
            return m2o.getDir(meta & mask);
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, OnlyNeedsMeta, WorldMutable, Config, Transformable -> true;
                default -> false;
            };
        }
    }
}
