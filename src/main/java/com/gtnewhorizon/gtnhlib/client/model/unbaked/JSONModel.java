package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.UNASSIGNED;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.fromForgeDir;
import static com.gtnewhorizon.gtnhlib.core.GTNHLibCore.MODEL_LOGGER;
import static com.gtnewhorizon.gtnhlib.util.DirectionUtil.rotateFacing;
import static org.joml.Math.fma;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.PileOfQuads;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Face;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.Position.ModelDisplay;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLoc;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadViewMutable;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class JSONModel implements UnbakedModel {

    @Nullable
    protected final ModelLoc parentId;
    @Nullable
    protected JSONModel parent;
    protected final boolean useAO;
    protected final Map<Position, ModelDisplay> display;
    @NotNull
    protected final Object2ObjectMap<String, String> textures;
    @NotNull
    protected List<ModelDeserializer.ModelElement> elements;

    protected static final Vector4f DEFAULT_UV = new Vector4f(0, 0, 16, 16);

    private static final Map<ForgeDirection, Matrix4f> UV_TRANSFORM_LOCAL_TO_GLOBAL = new EnumMap<>(
            ForgeDirection.class);

    private static final Map<ForgeDirection, Matrix4f> UV_TRANSFORM_GLOBAL_TO_LOCAL = new EnumMap<>(
            ForgeDirection.class);

    // Get these magic matrices
    static {
        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(ForgeDirection.SOUTH, new Matrix4f());

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(ForgeDirection.EAST, new Matrix4f().rotateY((float) (Math.PI / 2)));

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(ForgeDirection.WEST, new Matrix4f().rotateY((float) (-Math.PI / 2)));

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(ForgeDirection.NORTH, new Matrix4f().rotateY((float) Math.PI));

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(ForgeDirection.UP, new Matrix4f().rotateX((float) (-Math.PI / 2)));

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(ForgeDirection.DOWN, new Matrix4f().rotateX((float) (Math.PI / 2)));

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            UV_TRANSFORM_GLOBAL_TO_LOCAL
                    .put(direction, new Matrix4f(UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction)).invert());
        }
    }

    public JSONModel(@Nullable ModelLoc parentId, boolean useAO, Map<Position, ModelDisplay> display,
            @NotNull Object2ObjectMap<String, String> textures,
            @NotNull List<ModelDeserializer.ModelElement> elements) {
        this.parentId = parentId;
        this.useAO = useAO;
        this.display = display;
        this.textures = textures;
        this.elements = elements;
    }

    /**
     * Makes a shallow copy of og. This allows you to bake the same model multiple times with various transformations.
     */
    public JSONModel(JSONModel og) {

        this.parentId = og.parentId;
        this.parent = og.parent;
        this.useAO = og.useAO;
        this.display = og.display;
        this.textures = og.textures;
        this.elements = og.elements;
    }

    protected static void setUV(ModelQuadViewMutable q, int i, float u, float v) {
        q.setTexU(i, u);
        q.setTexV(i, v);
    }

    /**
     * Modern Minecraft uses magic arrays to do this without breaking AO. This is the same thing, but without arrays.
     * Note: still doesn't fix AO. Whoops.
     */
    @NotNull
    protected static Vector3f mapSideToVertex(Vector3f from, Vector3f to, int index, ForgeDirection side) {
        return switch (side) {
            case DOWN -> switch (index) {
                    case 0 -> new Vector3f(from.x, from.y, to.z);
                    case 1 -> new Vector3f(from.x, from.y, from.z);
                    case 2 -> new Vector3f(to.x, from.y, from.z);
                    case 3 -> new Vector3f(to.x, from.y, to.z);
                    default -> throw new RuntimeException("Too many indices!");
                };
            case UP -> switch (index) {
                    case 0 -> new Vector3f(from.x, to.y, from.z);
                    case 1 -> new Vector3f(from.x, to.y, to.z);
                    case 2 -> new Vector3f(to.x, to.y, to.z);
                    case 3 -> new Vector3f(to.x, to.y, from.z);
                    default -> throw new RuntimeException("Too many indices!");
                };
            case NORTH -> switch (index) {
                    case 0 -> new Vector3f(to.x, to.y, from.z);
                    case 1 -> new Vector3f(to.x, from.y, from.z);
                    case 2 -> new Vector3f(from.x, from.y, from.z);
                    case 3 -> new Vector3f(from.x, to.y, from.z);
                    default -> throw new RuntimeException("Too many indices!");
                };
            case SOUTH -> switch (index) {
                    case 0 -> new Vector3f(from.x, to.y, to.z);
                    case 1 -> new Vector3f(from.x, from.y, to.z);
                    case 2 -> new Vector3f(to.x, from.y, to.z);
                    case 3 -> new Vector3f(to.x, to.y, to.z);
                    default -> throw new RuntimeException("Too many indices!");
                };
            case WEST -> switch (index) {
                    case 0 -> new Vector3f(from.x, to.y, from.z);
                    case 1 -> new Vector3f(from.x, from.y, from.z);
                    case 2 -> new Vector3f(from.x, from.y, to.z);
                    case 3 -> new Vector3f(from.x, to.y, to.z);
                    default -> throw new RuntimeException("Too many indices!");
                };
            case EAST -> switch (index) {
                    case 0 -> new Vector3f(to.x, to.y, to.z);
                    case 1 -> new Vector3f(to.x, from.y, to.z);
                    case 2 -> new Vector3f(to.x, from.y, from.z);
                    case 3 -> new Vector3f(to.x, to.y, from.z);
                    default -> throw new RuntimeException("Too many indices!");
                };
            case UNKNOWN -> throw new IllegalArgumentException("No vector matching UNKNOWN!");
        };
    }

    @Override
    public BakedModel bake(BakeData data) {

        final var vRot = data.getAffineMatrix();
        final var sidedQuadStore = new HashMap<ModelQuadFacing, ArrayList<ModelQuadView>>(7);

        // Append faces from each element
        for (ModelDeserializer.ModelElement e : this.elements) {

            final Matrix4f rot = (e.rotation() == null) ? NOOP.getAffineMatrix() : e.rotation().getAffineMatrix();

            final Vector3f from = e.from();
            final Vector3f to = e.to();

            for (Face f : e.faces()) {

                // Assign vertexes
                final var quad = new ModelQuad();
                for (int i = 0; i < 4; ++i) {
                    final Vector3f vert = mapSideToVertex(from, to, i, f.name()).mulPosition(rot).mulPosition(vRot);
                    quad.setX(i, vert.x);
                    quad.setY(i, vert.y);
                    quad.setZ(i, vert.z);
                }

                // Set shading properties
                quad.setEmissiveness(e.lightEmission());
                quad.setDirectionalShading(e.shade());
                quad.setHasAmbientOcclusion(this.useAO);

                BakedUV bakedUV = f.bakedUV();

                if (data.uvLock()) {
                    bakedUV = recomputeUVs(f.bakedUV(), f.name(), new Matrix4f(data.getAffineMatrix()));
                }

                for (int i = 0; i < 4; i++) {
                    setUV(quad, i, bakedUV.getU(i), bakedUV.getV(i));
                }

                // Set the sprite
                var texKey = f.texture();
                var texName = textures.get(texKey);
                if (texName == null) {
                    MODEL_LOGGER.warn("Model {} has no texture for variable {}!", this, texKey);
                    texName = "minecraft:missing";
                }

                if (texName.startsWith("#")) {
                    MODEL_LOGGER.warn("Model {} has unflattened texture variable {} when baking!", this, texName);
                    textures.put(texKey, "minecraft:missing");
                    texName = "minecraft:missing";
                }
                bakeSprite(quad, texName);

                // Set the tint index
                quad.setColorIndex(f.tintIndex());

                // Rotate the cull face and lighting face
                final var cullFace = rotateFacing(fromForgeDir(f.cullFace()), vRot);
                final var lightFace = rotateFacing(fromForgeDir(f.name()), vRot);
                // Light face may not be unassigned
                quad.setLightFace(lightFace == UNASSIGNED ? POS_Y : lightFace);

                // Add the quad to the sided store
                sidedQuadStore.computeIfAbsent(cullFace, d -> new ArrayList<>()).add(quad);
            }
        }

        // Add them to the model
        return new PileOfQuads(sidedQuadStore, this.display, this.getParticle());
    }

    // The following functions are pretty much directly ported from vanilla used Matrix4f instead of transformation
    // just to keep it simpler.
    private static BakedUV recomputeUVs(BakedUV bakedUV, ForgeDirection face, Matrix4fc modelRotation) {
        // MODEL_LOGGER.info(
        // "UV FACE ROTATION: {} -> {}",
        // face,
        // rotate(modelRotation, face)
        // );

        Matrix4f transform = getUVLockTransform(modelRotation, face);

        MODEL_LOGGER.info("UV LOCK TRANSFORM face={}:\n{}", face, transform);

        float u = bakedUV.getU(bakedUV.getReverseIndex(0));
        float v = bakedUV.getV(bakedUV.getReverseIndex(0));

        Vector4f vector4f = transform.transform(new Vector4f(u / 16.0F, v / 16.0F, 0.0F, 1.0F));
        float f2 = 16.0F * vector4f.x();
        float f3 = 16.0F * vector4f.y();
        float f4 = bakedUV.getU(bakedUV.getReverseIndex(2));
        float f5 = bakedUV.getV(bakedUV.getReverseIndex(2));
        Vector4f vector4f1 = transform.transform(new Vector4f(f4 / 16.0F, f5 / 16.0F, 0.0F, 1.0F));
        float f6 = 16.0F * vector4f1.x();
        float f7 = 16.0F * vector4f1.y();
        float f8;
        float f9;
        if (Math.signum(f4 - u) == Math.signum(f6 - f2)) {
            f8 = f2;
            f9 = f6;
        } else {
            f8 = f6;
            f9 = f2;
        }

        float f10;
        float f11;
        if (Math.signum(f5 - v) == Math.signum(f7 - f3)) {
            f10 = f3;
            f11 = f7;
        } else {
            f10 = f7;
            f11 = f3;
        }

        float f12 = (float) Math.toRadians(bakedUV.rotation);
        Matrix3f matrix3f = new Matrix3f(transform);
        Vector3f vector3f = matrix3f.transform(new Vector3f(MathHelper.cos(f12), MathHelper.sin(f12), 0.0F));
        int i = Math.floorMod(
                -((int) Math.round(Math.toDegrees(Math.atan2((double) vector3f.y(), (double) vector3f.x())) / 90.0))
                        * 90,
                360);
        return new BakedUV(new Vector4f(f8, f10, f9, f11), i);
    }

    private static Matrix4f getUVLockTransform(Matrix4fc modelRotation, ForgeDirection face) {

        ForgeDirection rotatedFace = rotate(modelRotation, face);

        Matrix4f inverse = inverseOrIdentity(modelRotation);

        Matrix4f result = new Matrix4f(UV_TRANSFORM_GLOBAL_TO_LOCAL.get(face));

        result.mul(inverse);
        result.mul(UV_TRANSFORM_LOCAL_TO_GLOBAL.get(rotatedFace));

        return blockCenterToCorner(result);
    }

    private static Matrix4f inverseOrIdentity(Matrix4fc matrix) {
        if (Math.abs(matrix.determinant()) < 1.0e-6f) {
            GTNHLib.error("Failed to invert the UV transformation! Likely something wrong with scaling values.");
            return new Matrix4f();
        }

        return new Matrix4f(matrix).invert();
    }

    private static Matrix4f blockCenterToCorner(Matrix4f transform) {
        Matrix4f matrix = new Matrix4f().translation(0.5f, 0.5f, 0.5f);

        matrix.mul(transform);
        matrix.translate(-0.5f, -0.5f, -0.5f);

        return matrix;
    }

    private static ForgeDirection rotate(Matrix4fc matrix, ForgeDirection direction) {

        Vector3f normal = directionVector(direction);

        Vector4f transformed = matrix.transform(new Vector4f(normal.x, normal.y, normal.z, 0.0f));

        return getApproximateNearest(transformed.x, transformed.y, transformed.z);
    }

    private static ForgeDirection getApproximateNearest(float x, float y, float z) {

        ForgeDirection closest = ForgeDirection.NORTH;
        float best = -Float.MAX_VALUE;

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            Vector3f normal = directionVector(direction);

            float dot = x * normal.x + y * normal.y + z * normal.z;

            if (dot > best) {
                best = dot;
                closest = direction;
            }
        }

        return closest;
    }

    private static Vector3f directionVector(ForgeDirection direction) {
        return new Vector3f(direction.offsetX, direction.offsetY, direction.offsetZ);
    }

    protected void bakeSprite(ModelQuadViewMutable quad, String name) {
        name = name.replaceFirst("^minecraft:", "");
        final var icon = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
        final float minU = icon.getMinU();
        final float minV = icon.getMinV();
        final float dU = icon.getMaxU() - minU;
        final float dV = icon.getMaxV() - minV;
        quad.setSprite(icon);

        for (int i = 0; i < 4; ++i) {
            quad.setTexU(i, fma(dU, quad.getTexU(i) / 16, minU));
            quad.setTexV(i, fma(dV, quad.getTexV(i) / 16, minV));
        }
    }

    /// @return A JSON model which is the result of model resolution. Note that while this is *usually* the caller, it's
    /// not always - missing parents, for example, will propogate a missing model down.
    public JSONModel resolveParents(Function<ModelLoc, JSONModel> modelLoader) {

        if (this.parentId == null || this.parent != null) {
            return this;
        }

        // Inherit properties
        this.parent = modelLoader.apply(this.parentId).resolveParents(modelLoader);
        if (parent instanceof MissingModel) return parent;

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

        if (this.parent != null && this.parent.display != null) {
            for (Map.Entry<Position, ModelDisplay> e : this.parent.display.entrySet()) {
                this.display.putIfAbsent(e.getKey(), e.getValue());
            }
        }

        return this;
    }

    protected IIcon getParticle() {
        String key = "#particle";
        if (!textures.containsKey(key) && !textures.isEmpty()) {
            key = textures.keySet().iterator().next();
        }

        String texName = textures.getOrDefault(key, "missingno");
        texName = texName.replaceFirst("^minecraft:", "");

        return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texName);
    }

    public Map<String, String> getTextures() {
        return Object2ObjectMaps.unmodifiable(textures);
    }

    // Is this the right place for this?
    @Desugar
    public record BakedUV(Vector4f uv, int rotation) {

        public float getU(int shift) {
            if (this.uv == null) {
                throw new NullPointerException("uvs");
            } else {
                int i = this.getShiftedIndex(shift);
                return this.uv.get(i != 0 && i != 1 ? 2 : 0);
            }
        }

        public float getV(int shift) {
            if (this.uv == null) {
                throw new NullPointerException("uvs");
            } else {
                int i = this.getShiftedIndex(shift);
                return this.uv.get(i != 0 && i != 3 ? 3 : 1);
            }
        }

        private int getShiftedIndex(int shift) {
            return (shift + this.rotation / 90) % 4;
        }

        public int getReverseIndex(int shift) {
            return (shift + 4 - this.rotation / 90) % 4;
        }
    }
}
