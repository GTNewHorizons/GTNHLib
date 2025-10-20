package com.gtnewhorizon.gtnhlib.client.model.loading;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDisplay.Position;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelElement.Axis;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.util.JsonUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.util.ForgeDirection;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ModelDeserializer implements JsonDeserializer<JSONModel> {

    private static ForgeDirection fromName(String name) {
        return switch (name) {
            case "up" -> ForgeDirection.UP;
            case "down" -> ForgeDirection.DOWN;
            case "north" -> ForgeDirection.NORTH;
            case "south" -> ForgeDirection.SOUTH;
            case "west" -> ForgeDirection.WEST;
            case "east" -> ForgeDirection.EAST;
            case "unknown" -> ForgeDirection.UNKNOWN;
            default -> null;
        };
    }

    private Vector3f loadVec3(JsonObject in, String name) {

        final JsonArray arr = in.getAsJsonArray(name);
        final Vector3f ret = new Vector3f();

        for (int i = 0; i < 3; ++i) {
            ret.setComponent(i, arr.get(i).getAsFloat());
        }

        return ret;
    }

    private Vector3f loadVec3(JsonObject in, String name, Vector3f defaultv) {
        if (!in.isJsonArray()) return defaultv;
        return loadVec3(in, name);
    }

    private Vector4f loadVec4(JsonObject in, String name) {

        final JsonArray arr = in.getAsJsonArray(name);
        final Vector4f ret = new Vector4f();

        for (int i = 0; i < 4; ++i) {
            ret.setComponent(i, arr.get(i).getAsFloat());
        }

        return ret;
    }

    private ModelDisplay loadADisplay(JsonObject in) {

        final Vector3f rotation = loadVec3(in, "rotation", new Vector3f(0, 0, 0));
        final Vector3f translation = loadVec3(in, "translation", new Vector3f(0, 0, 0));
        final Vector3f scale = loadVec3(in, "scale", new Vector3f(1, 1, 1));

        return new ModelDisplay(rotation, translation, scale);
    }

    private Map<Position, ModelDisplay> loadDisplay(JsonObject in) {

        // wow such long
        final Map<Position, ModelDisplay> ret = new Object2ObjectOpenHashMap<>(Position.values().length);

        if (in.has("display")) {

            final JsonObject display = in.getAsJsonObject("display");

            for (Map.Entry<String, JsonElement> j : display.entrySet()) {

                final String name = j.getKey();
                final Position pos = Position.getByName(name);
                ret.put(pos, loadADisplay(j.getValue().getAsJsonObject()));
            }
        }

        for (Position p : Position.values()) {
            ret.putIfAbsent(p, ModelDisplay.DEFAULT);
        }

        return ret;
    }

    private Map<String, String> loadTextures(JsonObject in) {

        final Map<String, String> textures = new Object2ObjectOpenHashMap<>();

        if (in.has("textures")) {
            for (Map.Entry<String, JsonElement> e : in.getAsJsonObject("textures").entrySet()) {

                // Trim leading octothorpes. They indicate a texture variable, but I don't actually care.
                String s = e.getValue().getAsString();
                if (s.startsWith("#")) {
                    s = s.substring(1);
                } else if (s.startsWith("minecraft:")) {

                    // Because of how we fetch textures from the atlas, minecraft textures need to have their domain
                    // stripped
                    s = s.substring(10);
                }

                textures.put(e.getKey(), s);
            }
        }

        return textures;
    }

    private ModelElement.Rotation loadRotation(JsonObject in) {

        if (in.has("rotation")) {
            final JsonObject json = in.getAsJsonObject("rotation");

            final Vector3f origin = loadVec3(json, "origin").div(16);
            final Axis axis = Axis.fromName(JsonUtil.loadStr(json, "axis"));
            final float angle = JsonUtil.loadFloat(json, "angle");
            final boolean rescale = JsonUtil.loadBool(json, "rescale", false);

            return new ModelElement.Rotation(origin, axis, angle, rescale);
        } else {

            return null;
        }
    }

    private List<ModelElement.Face> loadFaces(JsonObject in) {

        final List<ModelElement.Face> ret = new ObjectArrayList<>();
        final JsonObject json = in.getAsJsonObject("faces");

        for (Map.Entry<String, JsonElement> e : json.entrySet()) {

            final ForgeDirection side = fromName(e.getKey());
            final JsonObject face = e.getValue().getAsJsonObject();

            final Vector4f uv = (face.has("uv")) ? loadVec4(face, "uv") : null;
            String texture = JsonUtil.loadStr(face, "texture");
            if (texture.startsWith("#")) texture = texture.substring(1);
            final ForgeDirection cullFace = fromName(JsonUtil.loadStr(face, "cullface", "unknown"));
            final int rotation = JsonUtil.loadInt(face, "rotation", 0);
            final int tintIndex = JsonUtil.loadInt(face, "tintindex", -1);

            ret.add(new ModelElement.Face(side, uv, texture, cullFace, rotation, tintIndex));
        }

        return ret;
    }

    private List<ModelElement> loadElements(JsonObject in) {

        final List<ModelElement> ret = new ObjectArrayList<>();

        if (in.has("elements")) {

            final JsonArray arr = in.getAsJsonArray("elements");
            for (JsonElement e : arr) {

                final JsonObject json = e.getAsJsonObject();
                final Vector3f from = loadVec3(json, "from").div(16);
                final Vector3f to = loadVec3(json, "to").div(16);
                final ModelElement.Rotation rotation = loadRotation(json);
                final boolean shade = JsonUtil.loadBool(json, "shade", true);
                final List<ModelElement.Face> faces = loadFaces(json);

                ret.add(new ModelElement(from, to, rotation, shade, faces));
            }
        }

        return ret;
    }

    @Override
    public JSONModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        final JsonObject in = json.getAsJsonObject();

        final String parent = JsonUtil.loadStr(in, "parent", null);
        ResourceLoc.ModelLoc parentId = null;
        if (parent != null && !parent.isEmpty()) {
            if (parent.contains(":")) {
                parentId = new ResourceLoc.ModelLoc(parent.split(":")[0], parent.split(":")[1]);
            } else {
                parentId = new ResourceLoc.ModelLoc("minecraft", parent);
            }
        }

        final boolean useAO = JsonUtil.loadBool(in, "ambientocclusion", true);
        final Map<Position, ModelDisplay> display = loadDisplay(in);
        final Map<String, String> textures = loadTextures(in);
        final List<ModelElement> elements = loadElements(in);

        return new JSONModel(parentId, useAO, display, textures, elements);
    }
}
