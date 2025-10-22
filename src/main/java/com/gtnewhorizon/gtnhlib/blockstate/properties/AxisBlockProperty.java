package com.gtnewhorizon.gtnhlib.blockstate.properties;

import java.lang.reflect.Type;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockPropertyTrait;
import com.gtnewhorizon.gtnhlib.blockstate.core.InvalidPropertyTextException;
import com.gtnewhorizon.gtnhlib.blockstate.core.MetaBlockProperty;
import com.gtnewhorizon.gtnhlib.blockstate.core.TransformableProperty;
import com.gtnewhorizon.gtnhlib.geometry.Axis;
import com.gtnewhorizon.gtnhlib.geometry.DirectionTransform;

public interface AxisBlockProperty extends BlockProperty<Axis>, TransformableProperty<Axis> {

    @Override
    default String getName() {
        return "axis";
    }

    @Override
    default Type getType() {
        return Axis.class;
    }

    @Override
    default JsonElement serialize(Axis value) {
        return new JsonPrimitive(stringify(value));
    }

    @Override
    default Axis deserialize(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? parse(element.getAsString())
                : Axis.UNKNOWN;
    }

    @Override
    default String stringify(Axis value) {
        return value.name().toLowerCase();
    }

    @Override
    default Axis parse(String text) throws InvalidPropertyTextException {
        try {
            return Axis.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyTextException("Invalid Axis", e);
        }
    }

    default boolean isValidAxis(Axis value) {
        return true;
    }

    @Override
    default @NotNull Axis transform(Axis value, DirectionTransform transform) {
        Axis axis = Axis.fromDirection(transform.apply(value.positive()));

        return isValidAxis(axis) ? axis : value;
    }

    interface Meta extends AxisBlockProperty, MetaBlockProperty<Axis> {

        @Override
        default boolean appliesTo(int meta) {
            return isValidAxis(getValue(meta));
        }
    }

    abstract class AbstractAxisBlockProperty implements Meta {

        private final String name;

        public AbstractAxisBlockProperty(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean hasTrait(BlockPropertyTrait trait) {
            return switch (trait) {
                case SupportsWorld, WorldMutable, OnlyNeedsMeta, Config, Transformable -> true;
                default -> false;
            };
        }
    }

    interface A2M {

        int getMeta(Axis axis);
    }

    interface M2A {

        Axis getAxis(int meta);
    }

    static AbstractAxisBlockProperty axis(int mask, A2M toMeta, M2A toAxis) {
        return new AbstractAxisBlockProperty("axis") {

            @Override
            public boolean needsExisting() {
                return mask != -1;
            }

            @Override
            public int getMeta(Axis value, int existing) {
                return toMeta.getMeta(value) | (existing & ~mask);
            }

            @Override
            public Axis getValue(int meta) {
                return toAxis.getAxis(meta & mask);
            }
        };
    }
}
