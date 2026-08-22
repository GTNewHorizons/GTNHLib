package com.gtnewhorizon.gtnhlib.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.BiConsumer;

import net.minecraft.network.PacketBuffer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;

public class JsonUtil {

    public static boolean loadBool(JsonObject in, String name, boolean defaul) {
        if (in.has(name)) return in.getAsJsonPrimitive(name).getAsBoolean();

        return defaul;
    }

    public static float loadFloat(JsonObject in, String name) {
        if (in.has(name)) return in.getAsJsonPrimitive(name).getAsFloat();

        throw new RuntimeException("Required field " + name + " not found in JsonObject " + in);
    }

    public static int loadInt(JsonObject in, String name, int defaul) {
        if (in.has(name)) return in.getAsJsonPrimitive(name).getAsInt();

        return defaul;
    }

    public static String loadStr(JsonObject in, String name) {
        if (in.has(name)) return in.getAsJsonPrimitive(name).getAsString();

        throw new RuntimeException("Required field " + name + " not found in JsonObject " + in);
    }

    public static String loadStr(JsonObject in, String name, String defaul) {
        if (in.has(name)) return in.getAsJsonPrimitive(name).getAsString();

        return defaul;
    }

    private static final int BSON_DOUBLE = 1;
    private static final int BSON_UTF8_STRING = 2;
    private static final int BSON_DOCUMENT = 3;
    private static final int BSON_ARRAY = 4;
    private static final int BSON_BOOLEAN = 8;
    private static final int BSON_NULL = 10;
    private static final int BSON_INT = 16;
    private static final int BSON_LONG = 18;

    public static void writeBSON(PacketBuffer buffer, JsonObject obj) {
        buffer.writeInt(Integer.reverseBytes(obj.entrySet().size()));

        for (var e : obj.entrySet()) {
            writeBSONElement(buffer, e.getKey(), e.getValue());
        }

        buffer.writeByte(0);
    }

    private static void writeBSONElement(PacketBuffer buffer, String ename, JsonElement el) {
        if (el.isJsonNull()) {
            buffer.writeByte(BSON_NULL);
            writeBsonCString(buffer, ename);
        } else if (el instanceof JsonPrimitive prim) {
            if (prim.isBoolean()) {
                buffer.writeByte(BSON_BOOLEAN);
                writeBsonCString(buffer, ename);

                buffer.writeByte(prim.getAsBoolean() ? 1 : 0);
            } else if (prim.isString()) {
                byte[] utf8 = prim.getAsString().getBytes(StandardCharsets.UTF_8);

                buffer.writeByte(BSON_UTF8_STRING);
                writeBsonCString(buffer, ename);

                buffer.writeInt(Integer.reverseBytes(utf8.length + 1));
                buffer.writeBytes(utf8);
                buffer.writeByte(0);
            } else {
                Class<? extends Number> type = prim.getAsNumber().getClass();

                if (type == Integer.class || type == Short.class) {
                    buffer.writeByte(BSON_INT);
                    writeBsonCString(buffer, ename);

                    buffer.writeInt(Integer.reverseBytes(prim.getAsInt()));
                } else if (type == Long.class || type == BigInteger.class) {
                    buffer.writeByte(BSON_LONG);
                    writeBsonCString(buffer, ename);

                    buffer.writeLong(Long.reverseBytes(prim.getAsLong()));
                } else if (type == Float.class || type == Double.class || type == BigDecimal.class) {
                    buffer.writeByte(BSON_DOUBLE);
                    writeBsonCString(buffer, ename);

                    buffer.writeLong(Long.reverseBytes(Double.doubleToLongBits(prim.getAsDouble())));
                } else {
                    // LazilyParsedNumber (Gson-parsed JSON) and other unknown Number types:
                    // inspect the string to decide int vs double.
                    String numStr = prim.getAsNumber().toString();
                    if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                        buffer.writeByte(BSON_DOUBLE);
                        writeBsonCString(buffer, ename);

                        buffer.writeLong(Long.reverseBytes(Double.doubleToLongBits(prim.getAsDouble())));
                    } else {
                        long lval = prim.getAsLong();
                        if (lval >= Integer.MIN_VALUE && lval <= Integer.MAX_VALUE) {
                            buffer.writeByte(BSON_INT);
                            writeBsonCString(buffer, ename);

                            buffer.writeInt(Integer.reverseBytes((int) lval));
                        } else {
                            buffer.writeByte(BSON_LONG);
                            writeBsonCString(buffer, ename);

                            buffer.writeLong(Long.reverseBytes(lval));
                        }
                    }
                }
            }
        } else if (el.isJsonArray()) {
            int key = 0;

            buffer.writeByte(BSON_ARRAY);
            writeBsonCString(buffer, ename);

            buffer.writeInt(Integer.reverseBytes(el.getAsJsonArray().size()));

            for (JsonElement el2 : el.getAsJsonArray()) {
                writeBSONElement(buffer, Integer.toString(key++), el2);
            }

            buffer.writeByte(0);
        } else if (el.isJsonObject()) {
            buffer.writeByte(BSON_DOCUMENT);
            writeBsonCString(buffer, ename);

            writeBSON(buffer, el.getAsJsonObject());
        }
    }

    public static JsonObject readBSON(PacketBuffer buffer) {
        int len = Integer.reverseBytes(buffer.readInt());

        JsonObject obj = new JsonObject();

        BiConsumer<String, JsonElement> adder = obj::add;

        for (int i = 0; i < len; i++) {
            readBSONElement(buffer, adder);
        }

        buffer.readByte();

        return obj;
    }

    private static void readBSONElement(PacketBuffer buffer, BiConsumer<String, JsonElement> adder) {
        int type = buffer.readByte();
        String ename = readBsonCString(buffer);

        switch (type) {
            case BSON_DOUBLE -> {
                adder.accept(ename, new JsonPrimitive(Double.longBitsToDouble(Long.reverseBytes(buffer.readLong()))));
            }
            case BSON_UTF8_STRING -> {
                int len = Integer.reverseBytes(buffer.readInt());

                byte[] data = new byte[len];

                buffer.readBytes(data);

                adder.accept(ename, new JsonPrimitive(new String(data, 0, len - 1, StandardCharsets.UTF_8)));
            }
            case BSON_DOCUMENT -> {
                adder.accept(ename, readBSON(buffer));
            }
            case BSON_ARRAY -> {
                int len = Integer.reverseBytes(buffer.readInt());

                JsonArray array = new JsonArray();

                BiConsumer<String, JsonElement> adder2 = (key, value) -> array.add(value);

                for (int i = 0; i < len; i++) {
                    readBSONElement(buffer, adder2);
                }

                buffer.readByte();

                adder.accept(ename, array);
            }
            case BSON_BOOLEAN -> {
                adder.accept(ename, new JsonPrimitive(buffer.readByte() != 0));
            }
            case BSON_NULL -> {
                adder.accept(ename, JsonNull.INSTANCE);
            }
            case BSON_INT -> {
                adder.accept(ename, new JsonPrimitive(Integer.reverseBytes(buffer.readInt())));
            }
            case BSON_LONG -> {
                adder.accept(ename, new JsonPrimitive(Long.reverseBytes(buffer.readLong())));
            }
            default -> {
                throw new IllegalStateException("Invalid bson code: " + type);
            }
        }
    }

    private static void writeBsonCString(PacketBuffer buffer, String str) {
        byte[] ascii = str.getBytes(StandardCharsets.UTF_8);

        for (byte b : ascii) {
            if (b == 0) {
                throw new IllegalArgumentException("cstring cannot contain 0: '" + str + "' / " + Arrays.toString(ascii));
            }
        }

        buffer.writeBytes(ascii);
        buffer.writeByte(0);
    }

    private static String readBsonCString(PacketBuffer buffer) {
        final ByteArrayList bytes = new ByteArrayList();

        byte b;

        while ((b = buffer.readByte()) != 0) {
            bytes.add(b);
        }

        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
}
