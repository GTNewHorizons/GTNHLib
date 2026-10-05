package com.gtnewhorizon.gtnhlib.codec.minecraft;

import static net.minecraftforge.common.util.Constants.NBT;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
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
import com.mojang.serialization.MapLike;

public final class NbtOps implements DynamicOps<NBTBase> {

    public static final NbtOps INSTANCE = new NbtOps();

    private static final NBTBase EMPTY = new NBTTagEnd();

    @Override
    public NBTBase empty() {
        return EMPTY;
    }

    @Override
    public NBTBase emptyList() {
        return new NBTTagList();
    }

    @Override
    public NBTBase emptyMap() {
        return new NBTTagCompound();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, NBTBase input) {
        return switch (input.getId()) {
            case NBT.TAG_END -> outOps.empty();
            case NBT.TAG_BYTE -> outOps.createByte(((NBTBase.NBTPrimitive) input).func_150290_f());
            case NBT.TAG_SHORT -> outOps.createShort(((NBTBase.NBTPrimitive) input).func_150289_e());
            case NBT.TAG_INT -> outOps.createInt(((NBTBase.NBTPrimitive) input).func_150287_d());
            case NBT.TAG_LONG -> outOps.createLong(((NBTBase.NBTPrimitive) input).func_150291_c());
            case NBT.TAG_FLOAT -> outOps.createFloat(((NBTBase.NBTPrimitive) input).func_150288_h());
            case NBT.TAG_DOUBLE -> outOps.createDouble(((NBTBase.NBTPrimitive) input).func_150286_g());
            case NBT.TAG_BYTE_ARRAY -> outOps
                    .createByteList(ByteBuffer.wrap(((NBTTagByteArray) input).func_150292_c()));
            case NBT.TAG_STRING -> outOps.createString(((NBTTagString) input).func_150285_a_());
            case NBT.TAG_LIST -> convertList(outOps, input);
            case NBT.TAG_COMPOUND -> convertMap(outOps, input);
            case NBT.TAG_INT_ARRAY -> outOps.createIntList(Arrays.stream(((NBTTagIntArray) input).func_150302_c()));
            default -> throw new IllegalStateException("Unknown NBT tag type: " + input.getId());
        };
    }

    @Override
    public DataResult<Number> getNumberValue(NBTBase input) {
        if (input instanceof NBTBase.NBTPrimitive primitive) {
            return DataResult.success(switch (input.getId()) {
                case NBT.TAG_BYTE -> primitive.func_150290_f();
                case NBT.TAG_SHORT -> primitive.func_150289_e();
                case NBT.TAG_INT -> primitive.func_150287_d();
                case NBT.TAG_LONG -> primitive.func_150291_c();
                case NBT.TAG_FLOAT -> primitive.func_150288_h();
                case NBT.TAG_DOUBLE -> primitive.func_150286_g();
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
        return createDouble(value.doubleValue());
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
        if (input instanceof NBTTagByte tag) {
            byte value = tag.func_150290_f();
            if (value == 0) return DataResult.success(false);
            if (value == 1) return DataResult.success(true);
        }
        return DataResult.error(() -> "Not a boolean: " + input);
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
        return mergeToList(list, Collections.singletonList(value));
    }

    @Override
    public DataResult<NBTBase> mergeToList(NBTBase list, List<NBTBase> values) {
        if (list instanceof NBTTagByteArray byteArray) return mergeToByteArray(byteArray, values);
        if (list instanceof NBTTagIntArray intArray) return mergeToIntArray(intArray, values);
        if (list.getId() == 0 || list instanceof NBTTagList) return mergeToTagList(list, values);
        return DataResult.error(() -> "Not a list: " + list, list);
    }

    private DataResult<NBTBase> mergeToByteArray(NBTTagByteArray list, List<NBTBase> values) {
        int added = 0;
        for (NBTBase value : values) {
            if (value.getId() == 0) continue;
            if (value.getId() != NBT.TAG_BYTE) {
                return DataResult
                        .error(() -> "Cannot add " + NBTBase.NBTTypes[value.getId()] + " to a byte array", list);
            }
            added++;
        }
        byte[] original = list.func_150292_c();
        byte[] result = Arrays.copyOf(original, original.length + added);
        int index = original.length;
        for (NBTBase value : values) {
            if (value.getId() != 0) result[index++] = ((NBTBase.NBTPrimitive) value).func_150290_f();
        }
        return DataResult.success(new NBTTagByteArray(result));
    }

    private DataResult<NBTBase> mergeToIntArray(NBTTagIntArray list, List<NBTBase> values) {
        int added = 0;
        for (NBTBase value : values) {
            if (value.getId() == 0) continue;
            if (value.getId() != NBT.TAG_INT) {
                return DataResult
                        .error(() -> "Cannot add " + NBTBase.NBTTypes[value.getId()] + " to an int array", list);
            }
            added++;
        }
        int[] original = list.func_150302_c();
        int[] result = Arrays.copyOf(original, original.length + added);
        int index = original.length;
        for (NBTBase value : values) {
            if (value.getId() != 0) result[index++] = ((NBTBase.NBTPrimitive) value).func_150287_d();
        }
        return DataResult.success(new NBTTagIntArray(result));
    }

    private DataResult<NBTBase> mergeToTagList(NBTBase list, List<NBTBase> values) {
        NBTTagList result = list instanceof NBTTagList ? (NBTTagList) list.copy() : new NBTTagList();
        for (NBTBase value : values) {
            if (value.getId() == 0) continue;
            if (result.tagCount() > 0 && result.func_150303_d() != value.getId()) {
                return DataResult.error(
                        () -> "Cannot add " + NBTBase.NBTTypes[value.getId()]
                                + " to a list of "
                                + NBTBase.NBTTypes[result.func_150303_d()],
                        list);
            }
            result.appendTag(value);
        }
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

    @Override
    public DataResult<NBTBase> mergeToMap(NBTBase map, MapLike<NBTBase> values) {
        if (map.getId() != 0 && !(map instanceof NBTTagCompound)) {
            return DataResult.error(() -> "Not a map: " + map, map);
        }

        NBTTagCompound result = map instanceof NBTTagCompound ? (NBTTagCompound) map.copy() : new NBTTagCompound();
        Iterator<Pair<NBTBase, NBTBase>> entries = values.entries().iterator();
        while (entries.hasNext()) {
            Pair<NBTBase, NBTBase> entry = entries.next();
            if (!(entry.getFirst() instanceof NBTTagString key)) {
                return DataResult.error(() -> "Map key is not a string: " + entry.getFirst(), result);
            }
            if (entry.getSecond().getId() != 0) result.setTag(key.func_150285_a_(), entry.getSecond());
        }
        return DataResult.success(result);
    }

    @Override
    public DataResult<Stream<Pair<NBTBase, NBTBase>>> getMapValues(NBTBase input) {
        if (input instanceof NBTTagCompound compound) {
            return DataResult.success(streamMapEntries(compound));
        }
        return DataResult.error(() -> "Not a map: " + input);
    }

    @SuppressWarnings("unchecked")
    private Stream<Pair<NBTBase, NBTBase>> streamMapEntries(NBTTagCompound compound) {
        Map<String, NBTBase> tags = (Map<String, NBTBase>) compound.tagMap;
        return tags.entrySet().stream().map(entry -> Pair.of(createString(entry.getKey()), entry.getValue()));
    }

    @Override
    public DataResult<MapLike<NBTBase>> getMap(NBTBase input) {
        if (input instanceof NBTTagCompound compound) {
            return DataResult.success(new MapLike<>() {

                @Override
                public NBTBase get(NBTBase key) {
                    return key instanceof NBTTagString stringKey ? get(stringKey.func_150285_a_()) : null;
                }

                @Override
                public NBTBase get(String key) {
                    return compound.getTag(key);
                }

                @Override
                public Stream<Pair<NBTBase, NBTBase>> entries() {
                    return streamMapEntries(compound);
                }
            });
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
        if (input instanceof NBTTagByteArray array) {
            byte[] bytes = array.func_150292_c().clone();
            return DataResult.success(streamArray(bytes.length, i -> createByte(bytes[i])));
        }
        if (input instanceof NBTTagIntArray array) {
            int[] values = array.func_150302_c().clone();
            return DataResult.success(streamArray(values.length, i -> createInt(values[i])));
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    private Stream<NBTBase> streamArray(int length, IntFunction<NBTBase> tagAt) {
        return IntStream.range(0, length).mapToObj(tagAt);
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
