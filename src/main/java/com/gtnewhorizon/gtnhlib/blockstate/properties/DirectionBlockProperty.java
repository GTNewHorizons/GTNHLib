package com.gtnewhorizon.gtnhlib.blockstate.properties;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UNKNOWN;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

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

public interface DirectionBlockProperty extends BlockProperty<ForgeDirection>, TransformableProperty<ForgeDirection> {

    @Override
    default String getName() {
        return "direction";
    }

    @Override
    default Type getType() {
        return ForgeDirection.class;
    }

    @Override
    default @NotNull ForgeDirection transform(ForgeDirection value, DirectionTransform transform) {
        ForgeDirection dir = transform.apply(value);

        return isValidDirection(dir) ? dir : value;
    }

    @Override
    default JsonElement serialize(ForgeDirection value) {
        return new JsonPrimitive(stringify(value));
    }

    @Override
    default ForgeDirection deserialize(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? parse(element.getAsString())
                : UNKNOWN;
    }

    @Override
    default String stringify(ForgeDirection value) {
        return value.name().toLowerCase();
    }

    @Override
    default ForgeDirection parse(String text) throws InvalidPropertyTextException {
        try {
            return ForgeDirection.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyTextException("Invalid ForgeDirection", e);
        }
    }

    default boolean isValidDirection(ForgeDirection value) {
        return true;
    }

    abstract class AbstractDirectionBlockProperty implements DirectionBlockProperty, MetaBlockProperty<ForgeDirection> {

        private String name;

        public AbstractDirectionBlockProperty(String name) {
            this.name = name;
        }

        public AbstractDirectionBlockProperty setName(String name) {
            this.name = name;
            return this;
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

    static DirectionBlockProperty facing() {
        return new AbstractDirectionBlockProperty("facing") {

            @Override
            public int getMeta(ForgeDirection value, int existing) {
                return value.ordinal();
            }

            @Override
            public ForgeDirection getValue(int meta) {
                return ForgeDirection.getOrientation(meta);
            }
        };
    }

    static AbstractDirectionBlockProperty facingVanilla(int mask) {
        return facing(mask, dir -> switch (dir) {
            case NORTH -> 3;
            case SOUTH -> 4;
            case WEST -> 1;
            case EAST -> 2;
            case UP -> 0;
            case DOWN -> 5;
            default -> 3;
        }, meta -> switch (meta) {
            case 3 -> NORTH;
            case 4 -> SOUTH;
            case 1 -> WEST;
            case 2 -> EAST;
            case 0 -> UP;
            case 5 -> DOWN;
            default -> NORTH;
        });
    }

    interface D2M {

        int getMeta(ForgeDirection dir);
    }

    interface M2D {

        ForgeDirection getDir(int meta);
    }

    static AbstractDirectionBlockProperty facing(int mask, D2M toMeta, M2D toDir) {
        return new AbstractDirectionBlockProperty("facing") {

            @Override
            public boolean needsExisting() {
                return mask != -1;
            }

            @Override
            public int getMeta(ForgeDirection value, int existing) {
                // If the mask == -1, the right side of this | becomes 0 regardless of what existing is.
                return toMeta.getMeta(value) | (existing & ~mask);
            }

            @Override
            public ForgeDirection getValue(int meta) {
                return toDir.getDir(meta & mask);
            }
        };
    }

    interface D2M2 {

        int getMeta(ForgeDirection dir, int existing);
    }

    static AbstractDirectionBlockProperty facing(D2M2 toMeta, M2D toDir) {
        return new AbstractDirectionBlockProperty("facing") {

            @Override
            public int getMeta(ForgeDirection value, int existing) {
                return toMeta.getMeta(value, existing);
            }

            @Override
            public ForgeDirection getValue(int meta) {
                return toDir.getDir(meta);
            }
        };
    }

    static AbstractDirectionBlockProperty facing(int mask, int north, int south, int west, int east, int up, int down) {
        return facing(mask, dir -> switch (dir) {
            case NORTH -> north == -1 ? 0 : north;
            case SOUTH -> south == -1 ? 0 : south;
            case WEST -> west == -1 ? 0 : west;
            case EAST -> east == -1 ? 0 : east;
            case UP -> up == -1 ? 0 : up;
            case DOWN -> down == -1 ? 0 : down;
            case UNKNOWN -> 0;
        }, meta -> {
            if (meta == north) return NORTH;
            if (meta == south) return SOUTH;
            if (meta == west) return WEST;
            if (meta == east) return EAST;
            if (meta == up) return UP;
            if (meta == down) return DOWN;

            return UNKNOWN;
        });
    }
}
