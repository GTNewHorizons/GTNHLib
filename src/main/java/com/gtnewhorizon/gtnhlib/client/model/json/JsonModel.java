package com.gtnewhorizon.gtnhlib.client.model.json;

import static com.gtnewhorizon.gtnhlib.client.renderer.util.DirectionUtil.ALL_DIRECTIONS;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.gtnewhorizon.gtnhlib.client.model.ModelVariant;
import com.gtnewhorizon.gtnhlib.client.model.NdQuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.Axis;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.Quad;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadProvider;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.util.DirectionUtil;
import com.gtnewhorizon.gtnhlib.util.JsonUtil;

import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.Getter;

public class JsonModel implements QuadProvider {

    @Nullable
    private final ResourceLocation parentId;
    @Nullable
    private JsonModel parent;
    @Getter
    private final boolean useAO;
    @Getter
    private final Map<ModelDisplay.Position, ModelDisplay> display;
    private final Map<String, String> textures;
    private List<ModelElement> elements;
    private List<QuadView> allQuadStore = new ObjectArrayList<>();
    private final Map<ForgeDirection, List<QuadView>> sidedQuadStore = new Object2ObjectOpenHashMap<>();
    private static final List<QuadView> EMPTY = ObjectImmutableList.of();

    public JsonModel(@Nullable ResourceLocation parentId, boolean useAO,
            Map<ModelDisplay.Position, ModelDisplay> display, Map<String, String> textures,
            List<ModelElement> elements) {
        this.parentId = parentId;
        this.useAO = useAO;
        this.display = display;
        this.textures = textures;
        this.elements = elements;
    }

    /**
     * Makes a shallow copy of og. This allows you to bake the same model multiple times with various transformations.
     */
    public JsonModel(JsonModel og) {

        this.parentId = og.parentId;
        this.parent = og.parent;
        this.useAO = og.useAO;
        this.display = og.display;
        this.textures = og.textures;
        this.elements = og.elements;
    }

    public void bake(ModelVariant v) {

        final Matrix4f vRot = v.getAffineMatrix();
        final NdQuadBuilder builder = new NdQuadBuilder();

        // Append faces from each element
        for (ModelElement e : this.elements) {

            final Matrix4f rot = (e.getRotation() == null) ? ModelElement.Rotation.NOOP.getAffineMatrix()
                    : e.getRotation().getAffineMatrix();

            final Vector3f from = e.getFrom();
            final Vector3f to = e.getTo();

            for (ModelElement.Face f : e.getFaces()) {

                float x = Float.MAX_VALUE;
                float y = Float.MAX_VALUE;
                float z = Float.MAX_VALUE;
                float X = Float.MIN_VALUE;
                float Y = Float.MIN_VALUE;
                float Z = Float.MIN_VALUE;

                // Assign vertexes
                for (int i = 0; i < 4; ++i) {

                    final Vector3f vert = QuadBuilder.mapSideToVertex(from, to, i, f.getName()).mulPosition(rot)
                            .mulPosition(vRot);
                    builder.pos(i, vert.x, vert.y, vert.z);
                    x = min(x, vert.x);
                    y = min(y, vert.y);
                    z = min(z, vert.z);
                    X = max(X, vert.x);
                    Y = max(Y, vert.y);
                    Z = max(Z, vert.z);
                }

                // Set culling and nominal faces
                builder.setCullFace();

                // Set bake flags
                int flags = switch (f.getRotation()) {
                    case 90 -> QuadBuilder.BAKE_ROTATE_90;
                    case 180 -> QuadBuilder.BAKE_ROTATE_180;
                    case 270 -> QuadBuilder.BAKE_ROTATE_270;
                    default -> QuadBuilder.BAKE_ROTATE_NONE;
                };

                // Set UV
                // TODO: UV locking
                final Vector4f uv = f.getUv();
                if (uv != null) {

                    builder.uv(0, uv.x, uv.y);
                    builder.uv(1, uv.x, uv.w);
                    builder.uv(2, uv.z, uv.w);
                    builder.uv(3, uv.z, uv.y);
                } else {

                    // Not sure if this is correct, but it seems to fix things
                    flags |= QuadBuilder.BAKE_LOCK_UV;
                }

                // Set the sprite
                builder.spriteBake(this.textures.get(f.getTexture()), flags);

                // Set the tint index
                builder.setColors(f.getTintIndex());

                // Set AO
                builder.mat.setAO(this.useAO);

                // Bake and add it
                final QuadView q = builder.build(new Quad());
                this.allQuadStore.add(q);
                this.sidedQuadStore.computeIfAbsent(q.getCullFace(), o -> new ObjectArrayList<>()).add(q);
            }
        }

        // Lock the lists.
        this.allQuadStore = new ObjectImmutableList<>(this.allQuadStore);
        for (ForgeDirection f : ALL_DIRECTIONS) {

            List<QuadView> l = this.sidedQuadStore.computeIfAbsent(f, o -> EMPTY);
            if (!l.isEmpty()) this.sidedQuadStore.put(f, new ObjectImmutableList<>(l));
        }
    }

    public List<ResourceLocation> getParents() {
        return Arrays.asList(parentId);
    }

    @Override
    public List<QuadView> getQuads(IBlockAccess world, int x, int y, int z, Block block, int meta, ForgeDirection dir,
            Random random, int color, Supplier<QuadView> quadPool) {

        return this.sidedQuadStore.getOrDefault(dir, EMPTY);
    }

    public void resolveParents(Function<ResourceLocation, JsonModel> modelLoader) {

        if (this.parentId != null && this.parent == null) {

            final JsonModel p = modelLoader.apply(this.parentId);
            p.resolveParents(modelLoader);

            // Inherit properties
            this.parent = p;
            if (this.elements.isEmpty()) this.elements = this.parent.elements;

            // Resolve texture variables
            // Add parent texture mappings, but prioritize ours.
            for (Map.Entry<String, String> e : this.parent.textures.entrySet()) {

                this.textures.putIfAbsent(e.getKey(), e.getValue());
            }

            // Flatten them, merging s -> s1, s1 -> s2 to s -> s2, s1 -> s2.
            boolean flat = false;
            final Map<String, String> tmp = new Object2ObjectOpenHashMap<>();
            while (!flat) {
                flat = true;

                for (Map.Entry<String, String> e : this.textures.entrySet()) {

                    // If there is a value in the key set, replace with the value it points to
                    // Also avoid adding a loop
                    if (this.textures.containsKey(e.getValue())) {

                        if (!e.getKey().equals(e.getValue())) tmp.put(e.getKey(), this.textures.get(e.getValue()));
                        else tmp.put(e.getKey(), "");
                        flat = false;
                    } else {
                        tmp.put(e.getKey(), e.getValue());
                    }
                }
                this.textures.putAll(tmp);
            }
        }
    }

    public static class Deserializer implements JsonDeserializer<JsonModel> {

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

        private Map<ModelDisplay.Position, ModelDisplay> loadDisplay(JsonObject in) {

            // wow such long
            final Map<ModelDisplay.Position, ModelDisplay> ret = new Object2ObjectOpenHashMap<>(
                    ModelDisplay.Position.values().length);

            if (in.has("display")) {

                final JsonObject display = in.getAsJsonObject("display");

                for (Map.Entry<String, JsonElement> j : display.entrySet()) {

                    final String name = j.getKey();
                    final ModelDisplay.Position pos = ModelDisplay.Position.getByName(name);
                    ret.put(pos, loadADisplay(j.getValue().getAsJsonObject()));
                }
            }

            for (ModelDisplay.Position p : ModelDisplay.Position.values()) {
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

                final ForgeDirection side = DirectionUtil.fromName(e.getKey());
                final JsonObject face = e.getValue().getAsJsonObject();

                final Vector4f uv = (face.has("uv")) ? loadVec4(face, "uv") : null;
                String texture = JsonUtil.loadStr(face, "texture");
                if (texture.startsWith("#")) texture = texture.substring(1);
                final ForgeDirection cullFace = DirectionUtil.fromName(JsonUtil.loadStr(face, "cullface", "unknown"));
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
        public JsonModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            final JsonObject in = json.getAsJsonObject();

            final String parent = JsonUtil.loadStr(in, "parent", "");
            ResourceLocation parentId = null;
            if (!parent.isEmpty()) {
                if (parent.contains(":")) {
                    parentId = new ModelLocation(parent.split(":")[0], parent.split(":")[1]);
                } else {
                    parentId = new ModelLocation(parent);
                }
            }

            final boolean useAO = JsonUtil.loadBool(in, "ambientocclusion", true);
            final Map<ModelDisplay.Position, ModelDisplay> display = loadDisplay(in);
            final Map<String, String> textures = loadTextures(in);
            final List<ModelElement> elements = loadElements(in);

            return new JsonModel(parentId, useAO, display, textures, elements);
        }
    }
}
