package com.gtnewhorizon.gtnhlib.util;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class NBTUtil {

    /**
     * Converts an nbt tag to json.
     * Does not preserve the specific types of the tags, but the returned data will be sane and generally correct.
     * Compatible with Gson.
     */
    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> T toJsonObject(NBTBase nbt) {
        if (nbt == null) return null;

        if (nbt instanceof NBTTagCompound tag) {
            final Map<String, NBTBase> tagMap = (Map<String, NBTBase>) tag.tagMap;

            JsonObject root = new JsonObject();

            for (Map.Entry<String, NBTBase> nbtEntry : tagMap.entrySet()) {
                root.add(nbtEntry.getKey(), toJsonObject(nbtEntry.getValue()));
            }

            return (T) root;
        } else if (nbt instanceof NBTTagByte x) {
            // Number (byte)
            return (T) new JsonPrimitive(x.func_150290_f());
        } else if (nbt instanceof NBTTagShort x) {
            // Number (short)
            return (T) new JsonPrimitive(x.func_150289_e());
        } else if (nbt instanceof NBTTagInt x) {
            // Number (int)
            return (T) new JsonPrimitive(x.func_150287_d());
        } else if (nbt instanceof NBTTagLong x) {
            // Number (long)
            return (T) new JsonPrimitive(x.func_150291_c());
        } else if (nbt instanceof NBTTagFloat x) {
            // Number (float)
            return (T) new JsonPrimitive(x.func_150288_h());
        } else if (nbt instanceof NBTTagDouble x) {
            // Number (double)
            return (T) new JsonPrimitive(x.func_150286_g());
        } else if (nbt instanceof NBTBase.NBTPrimitive x) {
            // Number
            return (T) new JsonPrimitive(x.func_150286_g());
        } else if (nbt instanceof NBTTagString str) {
            // String
            return (T) new JsonPrimitive(str.func_150285_a_());
        } else if (nbt instanceof NBTTagList list) {
            JsonArray arr = new JsonArray();
            list.tagList.forEach(c -> arr.add(toJsonObject((NBTBase) c)));
            return (T) arr;
        } else if (nbt instanceof NBTTagIntArray list) {
            JsonArray arr = new JsonArray();

            for (int i : list.func_150302_c()) {
                arr.add(new JsonPrimitive(i));
            }

            return (T) arr;
        } else if (nbt instanceof NBTTagByteArray list) {
            JsonArray arr = new JsonArray();

            for (byte i : list.func_150292_c()) {
                arr.add(new JsonPrimitive(i));
            }

            return (T) arr;
        } else {
            throw new IllegalArgumentException("Unsupported NBT Tag: " + NBTBase.NBTTypes[nbt.getId()] + " - " + nbt);
        }
    }

    /**
     * The opposite of {@link #toJsonObject(NBTBase)}
     */
    @SuppressWarnings("unchecked")
    public static <T extends NBTBase> T toNbt(JsonElement jsonElement) {
        if (jsonElement == null || jsonElement == JsonNull.INSTANCE) return null;

        if (jsonElement instanceof JsonPrimitive jsonPrimitive) {
            if (jsonPrimitive.isNumber()) {
                if (jsonPrimitive.toString().contains(".")) {
                        double dval = jsonPrimitive.getAsDouble();
                        float fval = (float) dval;

                        if (Math.abs(dval - fval) < 0.0001) return (T) new NBTTagFloat(fval);

                        return (T) new NBTTagDouble(dval);
                    } else {
                        long lval = jsonPrimitive.getAsLong();

                        if (lval >= Byte.MIN_VALUE && lval <= Byte.MAX_VALUE) return (T) new NBTTagByte((byte) lval);

                        if (lval >= Short.MIN_VALUE && lval <= Short.MAX_VALUE) return (T) new NBTTagShort((short) lval);

                        if (lval >= Integer.MIN_VALUE && lval <= Integer.MAX_VALUE) return (T) new NBTTagInt((int) lval);

                        return (T) new NBTTagLong(lval);
                    }
            } else {
                return (T) new NBTTagString(jsonPrimitive.getAsString());
            }
        } else if (jsonElement instanceof JsonArray jsonArray) {
            final List<NBTBase> tags = new ArrayList<>();

            int type = -1;

            for (JsonElement element : jsonArray) {
                if (element == null || element == JsonNull.INSTANCE) continue;

                NBTBase tag = toNbt(element);

                if (tag == null) continue;

                if (type == -1) type = tag.getId();
                if (type != tag.getId()) throw new IllegalArgumentException("NBT lists cannot contain tags of varying types");

                tags.add(tag);
            }

            // spotless:off
            if (type == Constants.NBT.TAG_INT) {
                return (T) new NBTTagIntArray(tags.stream().mapToInt(i -> ((NBTTagInt) i).func_150287_d()).toArray());
            } else if (type == Constants.NBT.TAG_BYTE) {
                final byte[] array = new byte[tags.size()];

                for (int i = 0; i < tags.size(); i++) {
                    array[i] = ((NBTTagByte) tags.get(i)).func_150290_f();
                }

                return (T) new NBTTagByteArray(array);
            } else {
                NBTTagList list = new NBTTagList();
                tags.forEach(list::appendTag);

                return (T) list;
            }
            // spotless:on
        } else if (jsonElement instanceof JsonObject jsonObject) {
            NBTTagCompound compound = new NBTTagCompound();

            for (Map.Entry<String, JsonElement> jsonEntry : jsonObject.entrySet()) {
                if (jsonEntry.getValue() == JsonNull.INSTANCE) continue;

                compound.setTag(jsonEntry.getKey(), toNbt(jsonEntry.getValue()));
            }

            return (T) compound;
        }

        throw new IllegalArgumentException("Unhandled element " + jsonElement);
    }

    /**
     * Converts an nbt tag to json.
     * Preserves types exactly. Not compatible with gson loading.
     */
    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> T toJsonObjectExact(NBTBase nbt) {
        if (nbt == null) {
            return null;
        }

        if (nbt instanceof NBTTagCompound tag) {
            final Map<String, NBTBase> tagMap = (Map<String, NBTBase>) tag.tagMap;

            JsonObject root = new JsonObject();

            for (Map.Entry<String, NBTBase> nbtEntry : tagMap.entrySet()) {
                root.add(nbtEntry.getKey(), toJsonObjectExact(nbtEntry.getValue()));
            }

            return (T) root;
        } else if (nbt instanceof NBTTagByte x) {
            return (T) new JsonPrimitive("b" + x.func_150290_f());
        } else if (nbt instanceof NBTTagShort x) {
            return (T) new JsonPrimitive("h" + x.func_150289_e());
        } else if (nbt instanceof NBTTagInt x) {
            return (T) new JsonPrimitive("i" + Integer.toUnsignedString(x.func_150287_d(), 16));
        } else if (nbt instanceof NBTTagLong x) {
            return (T) new JsonPrimitive("l" + Long.toUnsignedString(x.func_150291_c(), 16));
        } else if (nbt instanceof NBTTagFloat x) {
            return (T) new JsonPrimitive("f" + Long.toUnsignedString(Float.floatToIntBits(x.func_150288_h()), 16));
        } else if (nbt instanceof NBTTagDouble x) {
            return (T) new JsonPrimitive("d" + Long.toUnsignedString(Double.doubleToLongBits(x.func_150286_g()), 16));
        } else if (nbt instanceof NBTBase.NBTPrimitive other) {
            return (T) new JsonPrimitive("d" + Long.toUnsignedString(Double.doubleToLongBits(other.func_150286_g()), 16));
        } else if (nbt instanceof NBTTagString str) {
            return (T) new JsonPrimitive("s" + str.func_150285_a_());
        } else if (nbt instanceof NBTTagList list) {
            JsonArray arr = new JsonArray();

            list.tagList.forEach(c -> arr.add(toJsonObjectExact((NBTBase) c)));

            return (T) arr;
        } else if (nbt instanceof NBTTagIntArray array) {
            return (T) new JsonPrimitive(
                "1" + Base64.getEncoder()
                    .encodeToString(toByteArray(array.func_150302_c())));
        } else if (nbt instanceof NBTTagByteArray array) {
            return (T) new JsonPrimitive(
                "2" + Base64.getEncoder()
                    .encodeToString(array.func_150292_c()));
        } else {
            throw new IllegalArgumentException("Unsupported NBT Tag: " + NBTBase.NBTTypes[nbt.getId()] + " - " + nbt);
        }
    }

    /**
     * The opposite of {@link #toJsonObjectExact(NBTBase)}
     */
    public static NBTBase toNbtExact(JsonElement jsonElement) throws JsonParseException {
        if (jsonElement == null) return null;

        if (jsonElement instanceof JsonPrimitive primitive) {
            if (!primitive.isString())
                throw new JsonParseException("expected json primitive to be string: '" + primitive + "'");

            String data = primitive.getAsString();

            if (data.length() < 2) throw new JsonParseException("illegal json primitive string: '" + data + "'");

            char prefix = data.charAt(0);
            data = data.substring(1);

            try {
                switch (prefix) {
                    case 'b' -> {
                        return new NBTTagByte(Byte.parseByte(data));
                    }
                    case 'h' -> {
                        return new NBTTagShort(Short.parseShort(data));
                    }
                    case 'i' -> {
                        return new NBTTagInt(Integer.parseUnsignedInt(data, 16));
                    }
                    case 'l' -> {
                        return new NBTTagLong(Long.parseUnsignedLong(data, 16));
                    }
                    case 'f' -> {
                        return new NBTTagFloat(Float.intBitsToFloat((int) Long.parseUnsignedLong(data, 16)));
                    }
                    case 'd' -> {
                        return new NBTTagDouble(Double.longBitsToDouble(Long.parseUnsignedLong(data, 16)));
                    }
                    case 's' -> {
                        return new NBTTagString(data);
                    }
                    case '1' -> {
                        return new NBTTagIntArray(toIntArray(Base64.getDecoder().decode(data)));
                    }
                    case '2' -> {
                        return new NBTTagByteArray(
                            Base64.getDecoder()
                                .decode(data));
                    }
                }
            } catch (NumberFormatException e) {
                throw new JsonParseException("illegal number: " + primitive, e);
            }
        } else if (jsonElement instanceof JsonArray array) {
            NBTTagList list = new NBTTagList();

            for (JsonElement e : array) {
                list.appendTag(toNbtExact(e));
            }

            return list;
        } else if (jsonElement instanceof JsonObject obj) {
            NBTTagCompound tag = new NBTTagCompound();

            for (Map.Entry<String, JsonElement> jsonEntry : obj.entrySet()) {
                tag.setTag(jsonEntry.getKey(), toNbtExact(jsonEntry.getValue()));
            }

            return tag;
        }

        throw new IllegalArgumentException("Unhandled element " + jsonElement);
    }

    public static class ExactNBTTypeAdapter implements JsonSerializer<NBTBase>, JsonDeserializer<NBTBase> {

        public static final ExactNBTTypeAdapter INSTANCE = new ExactNBTTypeAdapter();

        private ExactNBTTypeAdapter() { }

        @Override
        public NBTBase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
            return toNbtExact(json);
        }

        @Override
        public JsonElement serialize(NBTBase src, Type typeOfSrc, JsonSerializationContext context) {
            return toJsonObjectExact(src);
        }
    }

    public static byte[] toByteArray(int[] array) {
        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);

        buffer.asIntBuffer().put(array);

        return buffer.array();
    }

    public static int[] toIntArray(byte[] array) {
        IntBuffer buffer = ByteBuffer.wrap(array).asIntBuffer();

        int[] data = new int[array.length / 4];
        buffer.get(data);

        return data;
    }
}
