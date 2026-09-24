package com.gtnewhorizon.gtnhlib.codec.minecraft;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagEnd;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public final class NbtOps implements DynamicOps<NBTBase> {

    public static final NbtOps INSTANCE = new NbtOps();

    @Override
    public NBTBase empty() {
        return new NBTTagEnd();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, NBTBase input) {
        return switch (input.getId()) {
            case 0 -> outOps.empty();
            case 1 -> outOps.createByte(((NBTBase.NBTPrimitive) input).func_150290_f());
            case 2 -> outOps.createShort(((NBTBase.NBTPrimitive) input).func_150289_e());
            case 3 -> outOps.createInt(((NBTBase.NBTPrimitive) input).func_150287_d());
            case 4 -> outOps.createLong(((NBTBase.NBTPrimitive) input).func_150291_c());
            case 5 -> outOps.createFloat(((NBTBase.NBTPrimitive) input).func_150288_h());
            case 6 -> outOps.createDouble(((NBTBase.NBTPrimitive) input).func_150286_g());
            case 7 -> outOps.createByteList(ByteBuffer.wrap(((NBTTagByteArray) input).func_150292_c()));
            case 8 -> outOps.createString(((NBTTagString) input).func_150285_a_());
            case 9 -> convertList(outOps, input);
            case 10 -> convertMap(outOps, input);
            case 11 -> outOps.createIntList(Arrays.stream(((NBTTagIntArray) input).func_150302_c()));
            default -> throw new IllegalStateException("Unknown NBT tag type: " + input.getId());
        };
    }

    @Override
    public DataResult<Number> getNumberValue(NBTBase input) {
        if (input instanceof NBTBase.NBTPrimitive primitive) {
            return DataResult.success(switch (input.getId()) {
                case 1 -> primitive.func_150290_f();
                case 2 -> primitive.func_150289_e();
                case 3 -> primitive.func_150287_d();
                case 4 -> primitive.func_150291_c();
                case 5 -> primitive.func_150288_h();
                case 6 -> primitive.func_150286_g();
                default -> throw new IllegalStateException("Unknown numeric NBT tag type: " + input.getId());
            });
        }
        return DataResult.error(() -> "Not a number: " + input);
    }

    @Override
    public NBTBase createNumeric(Number value) {
        if (value instanceof Byte) return createByte(value.byteValue());
        if (value instanceof Short) return createShort(value.shortValue());
        if (value instanceof Integer) return createInt(value.intValue());
        if (value instanceof Long) return createLong(value.longValue());
        if (value instanceof Float) return createFloat(value.floatValue());
        if (value instanceof Double) return createDouble(value.doubleValue());
        return inferNumeric(value);
    }

    // Gson hands over a LazilyParsedNumber, so a whole number has to be told apart from a fraction by its text
    private NBTBase inferNumeric(Number value) {
        String text = value.toString();
        if (text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0) {
            return createDouble(value.doubleValue());
        }
        try {
            long whole = Long.parseLong(text);
            return whole == (int) whole ? createInt((int) whole) : createLong(whole);
        } catch (NumberFormatException tooBig) {
            return createDouble(value.doubleValue());
        }
    }

    @Override
    public NBTBase createByte(byte value) {
        return new NBTTagByte(value);
    }

    @Override
    public NBTBase createShort(short value) {
        return new NBTTagShort(value);
    }

    @Override
    public NBTBase createInt(int value) {
        return new NBTTagInt(value);
    }

    @Override
    public NBTBase createLong(long value) {
        return new NBTTagLong(value);
    }

    @Override
    public NBTBase createFloat(float value) {
        return new NBTTagFloat(value);
    }

    @Override
    public NBTBase createDouble(double value) {
        return new NBTTagDouble(value);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(NBTBase input) {
        return getNumberValue(input).map(number -> number.byteValue() != 0);
    }

    @Override
    public NBTBase createBoolean(boolean value) {
        return createByte((byte) (value ? 1 : 0));
    }

    @Override
    public DataResult<String> getStringValue(NBTBase input) {
        if (input instanceof NBTTagString string) return DataResult.success(string.func_150285_a_());
        return DataResult.error(() -> "Not a string: " + input);
    }

    @Override
    public NBTBase createString(String value) {
        return new NBTTagString(value);
    }

    @Override
    public DataResult<NBTBase> mergeToList(NBTBase list, NBTBase value) {
        if (list.getId() != 0 && !(list instanceof NBTTagList)) {
            return DataResult.error(() -> "Not a list: " + list, list);
        }

        NBTTagList result = list instanceof NBTTagList ? (NBTTagList) list.copy() : new NBTTagList();
        if (value.getId() == 0) return DataResult.success(result);
        if (result.tagCount() > 0 && result.func_150303_d() != value.getId()) {
            return DataResult.error(
                    () -> "Cannot add " + NBTBase.NBTTypes[value.getId()]
                            + " to a list of "
                            + NBTBase.NBTTypes[result.func_150303_d()],
                    list);
        }
        result.appendTag(value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<NBTBase> mergeToMap(NBTBase map, NBTBase key, NBTBase value) {
        if (map.getId() != 0 && !(map instanceof NBTTagCompound)) {
            return DataResult.error(() -> "Not a map: " + map, map);
        }
        if (!(key instanceof NBTTagString stringKey)) {
            return DataResult.error(() -> "Map key is not a string: " + key, map);
        }

        NBTTagCompound result = map instanceof NBTTagCompound ? (NBTTagCompound) map.copy() : new NBTTagCompound();
        if (value.getId() != 0) result.setTag(stringKey.func_150285_a_(), value);
        return DataResult.success(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataResult<Stream<Pair<NBTBase, NBTBase>>> getMapValues(NBTBase input) {
        if (input instanceof NBTTagCompound compound) {
            Map<String, NBTBase> nbtBaseMap = (Map<String, NBTBase>) compound.tagMap.entrySet();
            return DataResult.success(
                    nbtBaseMap.entrySet().stream()
                            .map(entry -> Pair.of(createString(entry.getKey()), entry.getValue())));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public NBTBase createMap(Stream<Pair<NBTBase, NBTBase>> input) {
        NBTTagCompound result = new NBTTagCompound();
        input.forEach(entry -> {
            if (!(entry.getFirst() instanceof NBTTagString key)) {
                throw new IllegalArgumentException("Map key is not a string: " + entry.getFirst());
            }
            if (entry.getSecond().getId() != 0) result.setTag(key.func_150285_a_(), entry.getSecond());
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataResult<Stream<NBTBase>> getStream(NBTBase input) {
        if (input instanceof NBTTagList list) {
            return DataResult.success(list.tagList.stream());
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public NBTBase createList(Stream<NBTBase> input) {
        NBTTagList result = new NBTTagList();
        input.filter(tag -> tag.getId() != 0).forEach(tag -> {
            if (result.tagCount() > 0 && result.func_150303_d() != tag.getId()) {
                throw new IllegalArgumentException(
                        "Cannot add " + NBTBase.NBTTypes[tag.getId()]
                                + " to a list of "
                                + NBTBase.NBTTypes[result.func_150303_d()]);
            }
            result.appendTag(tag);
        });
        return result;
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(NBTBase input) {
        if (input instanceof NBTTagByteArray array) {
            byte[] bytes = array.func_150292_c();
            return DataResult.success(ByteBuffer.wrap(bytes.clone()));
        }
        return DynamicOps.super.getByteBuffer(input);
    }

    @Override
    public NBTBase createByteList(ByteBuffer input) {
        ByteBuffer copy = input.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return new NBTTagByteArray(bytes);
    }

    @Override
    public DataResult<IntStream> getIntStream(NBTBase input) {
        if (input instanceof NBTTagIntArray array) {
            int[] values = array.func_150302_c();
            return DataResult.success(Arrays.stream(values.clone()));
        }
        return DynamicOps.super.getIntStream(input);
    }

    @Override
    public NBTBase createIntList(IntStream input) {
        return new NBTTagIntArray(input.toArray());
    }

    @Override
    public NBTBase remove(NBTBase input, String key) {
        if (!(input instanceof NBTTagCompound compound)) return input;
        NBTTagCompound result = (NBTTagCompound) compound.copy();
        result.removeTag(key);
        return result;
    }

    @Override
    public String toString() {
        return "NBT";
    }
}
