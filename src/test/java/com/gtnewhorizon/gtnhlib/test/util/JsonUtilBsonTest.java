package com.gtnewhorizon.gtnhlib.test.util;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.util.JsonUtil;

public class JsonUtilBsonTest {

    private static PacketBuffer newBuffer() {
        return new PacketBuffer(Unpooled.buffer());
    }

    // --- helpers ---

    private static JsonObject roundtrip(JsonObject obj) {
        PacketBuffer buf = newBuffer();
        JsonUtil.writeBSON(buf, obj);
        return JsonUtil.readBSON(buf);
    }

    // --- null ---

    @Test
    void roundtrip_null() {
        JsonObject obj = new JsonObject();
        obj.add("k", JsonNull.INSTANCE);

        JsonObject result = roundtrip(obj);
        assertTrue(result.get("k").isJsonNull());
    }

    // --- boolean ---

    @Test
    void roundtrip_boolean_true() {
        JsonObject obj = new JsonObject();
        obj.add("v", new JsonPrimitive(true));
        assertTrue(roundtrip(obj).get("v").getAsBoolean());
    }

    @Test
    void roundtrip_boolean_false() {
        JsonObject obj = new JsonObject();
        obj.add("v", new JsonPrimitive(false));
        assertFalse(roundtrip(obj).get("v").getAsBoolean());
    }

    // --- string ---

    @Test
    void roundtrip_string() {
        JsonObject obj = new JsonObject();
        obj.add("s", new JsonPrimitive("hello"));
        // BUG: current impl includes the BSON null terminator in the string length
        // field but then reads all `len` bytes into the output, yielding "hello\0".
        assertEquals("hello", roundtrip(obj).get("s").getAsString());
    }

    @Test
    void roundtrip_string_empty() {
        JsonObject obj = new JsonObject();
        obj.add("s", new JsonPrimitive(""));
        // BUG: same null-terminator issue — empty string becomes "\0".
        assertEquals("", roundtrip(obj).get("s").getAsString());
    }

    @Test
    void roundtrip_string_unicode() {
        JsonObject obj = new JsonObject();
        obj.add("s", new JsonPrimitive("héllo wörld \u4e2d\u6587"));
        assertEquals("héllo wörld \u4e2d\u6587", roundtrip(obj).get("s").getAsString());
    }

    // --- int ---

    @Test
    void roundtrip_int() {
        JsonObject obj = new JsonObject();
        obj.add("zero", new JsonPrimitive(0));
        obj.add("pos", new JsonPrimitive(42));
        obj.add("neg", new JsonPrimitive(-1));
        obj.add("min", new JsonPrimitive(Integer.MIN_VALUE));
        obj.add("max", new JsonPrimitive(Integer.MAX_VALUE));

        JsonObject r = roundtrip(obj);
        assertEquals(0, r.get("zero").getAsInt());
        assertEquals(42, r.get("pos").getAsInt());
        assertEquals(-1, r.get("neg").getAsInt());
        assertEquals(Integer.MIN_VALUE, r.get("min").getAsInt());
        assertEquals(Integer.MAX_VALUE, r.get("max").getAsInt());
    }

    // --- long ---

    @Test
    void roundtrip_long() {
        JsonObject obj = new JsonObject();
        obj.add("v", new JsonPrimitive(123456789012345L));
        obj.add("min", new JsonPrimitive(Long.MIN_VALUE));
        obj.add("max", new JsonPrimitive(Long.MAX_VALUE));

        JsonObject r = roundtrip(obj);
        assertEquals(123456789012345L, r.get("v").getAsLong());
        assertEquals(Long.MIN_VALUE, r.get("min").getAsLong());
        assertEquals(Long.MAX_VALUE, r.get("max").getAsLong());
    }

    // --- double ---

    @Test
    void roundtrip_double() {
        JsonObject obj = new JsonObject();
        obj.add("pi", new JsonPrimitive(Math.PI));
        obj.add("neg", new JsonPrimitive(-2.718281828));
        obj.add("nan", new JsonPrimitive(Double.NaN));
        obj.add("inf", new JsonPrimitive(Double.POSITIVE_INFINITY));

        JsonObject r = roundtrip(obj);
        assertEquals(Math.PI, r.get("pi").getAsDouble());
        assertEquals(-2.718281828, r.get("neg").getAsDouble());
        assertEquals(Double.NaN, r.get("nan").getAsDouble());
        assertEquals(Double.POSITIVE_INFINITY, r.get("inf").getAsDouble());
    }

    // --- float (stored as BSON_DOUBLE) ---

    @Test
    void roundtrip_float() {
        JsonObject obj = new JsonObject();
        obj.add("v", new JsonPrimitive(1.5f));

        JsonObject r = roundtrip(obj);
        assertEquals(1.5f, r.get("v").getAsFloat(), 1e-7f);
    }

    // --- nested object ---

    @Test
    void roundtrip_nested_object() {
        JsonObject inner = new JsonObject();
        inner.add("x", new JsonPrimitive(10));

        JsonObject obj = new JsonObject();
        obj.add("inner", inner);

        JsonObject r = roundtrip(obj);
        JsonObject rInner = r.getAsJsonObject("inner");
        assertNotNull(rInner);
        assertEquals(10, rInner.get("x").getAsInt());
    }

    // --- array ---

    @Test
    void roundtrip_array_of_ints() {
        JsonArray arr = new JsonArray();
        arr.add(new JsonPrimitive(1));
        arr.add(new JsonPrimitive(2));
        arr.add(new JsonPrimitive(3));

        JsonObject obj = new JsonObject();
        obj.add("arr", arr);

        JsonArray r = roundtrip(obj).getAsJsonArray("arr");
        assertNotNull(r);
        assertEquals(3, r.size());
        assertEquals(1, r.get(0).getAsInt());
        assertEquals(2, r.get(1).getAsInt());
        assertEquals(3, r.get(2).getAsInt());
    }

    @Test
    void roundtrip_empty_array() {
        JsonObject obj = new JsonObject();
        obj.add("arr", new JsonArray());

        JsonArray r = roundtrip(obj).getAsJsonArray("arr");
        assertNotNull(r);
        assertEquals(0, r.size());
    }

    @Test
    void roundtrip_array_mixed_types() {
        JsonArray arr = new JsonArray();
        arr.add(JsonNull.INSTANCE);
        arr.add(new JsonPrimitive(true));
        arr.add(new JsonPrimitive(7));

        JsonObject obj = new JsonObject();
        obj.add("arr", arr);

        JsonArray r = roundtrip(obj).getAsJsonArray("arr");
        assertTrue(r.get(0).isJsonNull());
        assertTrue(r.get(1).getAsBoolean());
        assertEquals(7, r.get(2).getAsInt());
    }

    // --- empty top-level object ---

    @Test
    void roundtrip_empty_object() {
        assertEquals(0, roundtrip(new JsonObject()).entrySet().size());
    }

    // --- complex nested ---

    @Test
    void roundtrip_complex() {
        JsonArray arr = new JsonArray();
        arr.add(new JsonPrimitive(99L));
        arr.add(JsonNull.INSTANCE);

        JsonObject inner = new JsonObject();
        inner.add("flag", new JsonPrimitive(false));

        JsonObject obj = new JsonObject();
        obj.add("count", new JsonPrimitive(5));
        obj.add("arr", arr);
        obj.add("sub", inner);
        obj.add("nullVal", JsonNull.INSTANCE);

        JsonObject r = roundtrip(obj);
        assertEquals(5, r.get("count").getAsInt());
        assertTrue(r.get("nullVal").isJsonNull());
        assertFalse(r.getAsJsonObject("sub").get("flag").getAsBoolean());

        JsonArray rArr = r.getAsJsonArray("arr");
        assertEquals(99L, rArr.get(0).getAsLong());
        assertTrue(rArr.get(1).isJsonNull());
    }

    // --- LazilyParsedNumber: numbers from a parsed JSON string ---

    @Test
    void roundtrip_parsed_json_int() {
        // Gson stores parsed numbers as LazilyParsedNumber, not Integer/Long/etc.
        // writeBSONElement's class-based switch falls through all cases for
        // LazilyParsedNumber, silently dropping the element.
        // This test documents that the roundtrip must preserve the value.
        JsonObject obj = new Gson().fromJson("{\"n\": 42}", JsonObject.class);

        JsonObject r = roundtrip(obj);
        assertTrue(r.has("n"), "parsed int must survive roundtrip (LazilyParsedNumber bug)");
        assertEquals(42, r.get("n").getAsInt());
    }

    @Test
    void roundtrip_parsed_json_float() {
        JsonObject obj = new Gson().fromJson("{\"v\": 1.5}", JsonObject.class);

        JsonObject r = roundtrip(obj);
        assertTrue(r.has("v"), "parsed float must survive roundtrip (LazilyParsedNumber bug)");
        assertEquals(1.5, r.get("v").getAsDouble(), 1e-9);
    }
}
