package com.gtnewhorizon.gtnhlib.client.model.loading;

import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis.X;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis.Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis.Z;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position.ModelDisplay;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.Axis;
import com.gtnewhorizon.gtnhlib.util.JsonUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraftforge.common.util.ForgeDirection;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
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

    private static Vector3f loadVec3(JsonObject in, String name) {

        final JsonArray arr = in.getAsJsonArray(name);
        final Vector3f ret = new Vector3f();

        for (int i = 0; i < 3; ++i) {
            ret.setComponent(i, arr.get(i).getAsFloat());
        }

        return ret;
    }

    private static Vector3f loadVec3(JsonObject in, String name, Vector3f defaultv) {
        if (!in.isJsonArray()) return defaultv;
        return loadVec3(in, name);
    }

    private static Vector4f loadVec4(JsonObject in, String name) {

        final JsonArray arr = in.getAsJsonArray(name);
        final Vector4f ret = new Vector4f();

        for (int i = 0; i < 4; ++i) {
            ret.setComponent(i, arr.get(i).getAsFloat());
        }

        return ret;
    }

    private static ModelDisplay loadADisplay(JsonObject in) {

        final Vector3f rotation = loadVec3(in, "rotation", new Vector3f(0, 0, 0));
        final Vector3f translation = loadVec3(in, "translation", new Vector3f(0, 0, 0));
        final Vector3f scale = loadVec3(in, "scale", new Vector3f(1, 1, 1));

        return new ModelDisplay(rotation, translation, scale);
    }

    private static Map<Position, ModelDisplay> loadDisplay(JsonObject in) {

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

    private static Pattern TEXEX = Pattern.compile("^([^:]+:)block/");
    private static Object2ObjectOpenHashMap<String, String> loadTextures(JsonObject in) {

        final var textures = new Object2ObjectOpenHashMap<String, String>();

        if (in.has("textures")) {
            for (Map.Entry<String, JsonElement> e : in.getAsJsonObject("textures").entrySet()) {
                String s = e.getValue().getAsString();

                // If it's a texture variable, no munging is needed
                if (!s.startsWith("#")) {
                    // Add the default domain, if it's absent.
                    if (!s.contains(":")) s = "minecraft:" + s;

                    // Strip "block/" if present
                    s = TEXEX.matcher(s).replaceFirst("$1");
                }

                // The key is always a variable, so prepend accordingly
                final var key = e.getKey();
                textures.put(key.startsWith("#") ? key : "#" + key, s);
            }
        }

        return textures;
    }

    private static ModelElement.Rotation loadRotation(JsonObject in) {

        if (in.has("rotation")) {
            final JsonObject json = in.getAsJsonObject("rotation");

            final Vector3f origin = loadVec3(json, "origin").div(16);
            final String sAxis = JsonUtil.loadStr(json, "axis");
            final Axis axis = switch (sAxis) {
                case "x" -> X;
                case "y" -> Y;
                case "z" -> Z;
                default -> throw new JsonParseException("Invalid axis " + sAxis);
            };
            final float angle = JsonUtil.loadFloat(json, "angle");
            final boolean rescale = JsonUtil.loadBool(json, "rescale", false);

            return new ModelElement.Rotation(origin, axis, angle, rescale);
        } else {

            return null;
        }
    }

    private static List<ModelElement.Face> loadFaces(JsonObject in) {

        final List<ModelElement.Face> ret = new ObjectArrayList<>();
        final JsonObject json = in.getAsJsonObject("faces");

        for (Map.Entry<String, JsonElement> e : json.entrySet()) {

            final ForgeDirection side = fromName(e.getKey());
            final JsonObject face = e.getValue().getAsJsonObject();

            final Vector4f uv = (face.has("uv")) ? loadVec4(face, "uv") : null;
            String texture = JsonUtil.loadStr(face, "texture");
            final ForgeDirection cullFace = fromName(JsonUtil.loadStr(face, "cullface", "unknown"));
            final int rotation = JsonUtil.loadInt(face, "rotation", 0);
            final int tintIndex = JsonUtil.loadInt(face, "tintindex", -1);

            ret.add(new ModelElement.Face(side, uv, texture, cullFace, rotation, tintIndex));
        }

        return ret;
    }

    private static List<ModelElement> loadElements(JsonObject in) {

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
        final var textures = loadTextures(in);
        final List<ModelElement> elements = loadElements(in);

        return new JSONModel(parentId, useAO, display, textures, elements);
    }

    public enum Position {

        THIRDPERSON_RIGHTHAND,
        THIRDPERSON_LEFTHAND,
        FIRSTPERSON_RIGHTHAND,
        FIRSTPERSON_LEFTHAND,
        GUI, // inventory
        HEAD,
        GROUND, // dropped item I think
        FIXED; // item frames

        public static Position getByName(String name) {
            return switch (name) {
                case "thirdperson_righthand" -> THIRDPERSON_RIGHTHAND;
                case "thirdperson_lefthand" -> THIRDPERSON_LEFTHAND;
                case "firstperson_righthand" -> FIRSTPERSON_RIGHTHAND;
                case "firstperson_lefthand" -> FIRSTPERSON_LEFTHAND;
                case "gui" -> GUI;
                case "head" -> HEAD;
                case "ground" -> GROUND;
                case "fixed" -> FIXED;
                default -> null;
            };
        }

        @Desugar
        public record ModelDisplay(Vector3f rotation, Vector3f translation, Vector3f scale) {

            public static final ModelDisplay DEFAULT = new ModelDisplay(
                    new Vector3f(0, 0, 0),
                    new Vector3f(0, 0, 0),
                    new Vector3f(1, 1, 1));

        }
    }

    @Desugar
    public record ModelElement(Vector3f from, Vector3f to, @Nullable ModelDeserializer.ModelElement.Rotation rotation,
            boolean shade, List<Face> faces) {

        @Desugar
        public record Face(ForgeDirection name, Vector4f uv, String texture, ForgeDirection cullFace, int rotation,
                int tintIndex) {}

        @Desugar
        public record Rotation(Vector3f origin, Axis axis, float angle, boolean rescale) {

            public static final Rotation NOOP = new Rotation(new Vector3f(0, 0, 0), X, 0, false);

            public Rotation(Vector3f origin, Axis axis, float angle, boolean rescale) {
                this.origin = origin;
                this.axis = axis;
                this.angle = (float) Math.toRadians(angle);
                this.rescale = rescale;
            }

            public Matrix4f getAffineMatrix() {

                // Subtract origin
                final Matrix4f ret = new Matrix4f().translation(-this.origin.x, -this.origin.y, -this.origin.z);

                // Rotate
                switch (this.axis) {
                    case X -> ret.rotateLocalX(angle);
                    case Y -> ret.rotateLocalY(angle);
                    case Z -> ret.rotateLocalZ(angle);
                }

                // Add the origin back in
                return ret.translateLocal(this.origin.x, this.origin.y, this.origin.z);
            }
        }
    }
}
