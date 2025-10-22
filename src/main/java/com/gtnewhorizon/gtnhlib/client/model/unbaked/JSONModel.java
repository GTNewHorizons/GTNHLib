package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.POS_Y;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.UNASSIGNED;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.joml.Math.fma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.base.Objects;
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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;

public class JSONModel implements UnbakedModel {

    @Nullable
    private final ModelLocation parentId;
    @Nullable
    private JSONModel parent;
    @Getter
    private final boolean useAO;
    @Getter
    private final Map<Position, ModelDisplay> display;
    @NotNull
    private final Map<String, String> textures;
    private List<ModelDeserializer.ModelElement> elements;

    private static final Vector4f DEFAULT_UV = new Vector4f(0, 0, 16, 16);

    public JSONModel(@Nullable ModelLocation parentId, boolean useAO, Map<Position, ModelDisplay> display,
            @NotNull Map<String, String> textures, List<ModelDeserializer.ModelElement> elements) {
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

    private static void setUV(ModelQuadViewMutable q, int i, float u, float v) {
        q.setTexU(i, u);
        q.setTexV(i, v);
    }

    /**
     * Modern Minecraft uses magic arrays to do this without breaking AO. This is the same thing, but without arrays.
     * Note: still doesn't fix AO. Whoops.
     */
    @NotNull
    private static Vector3f mapSideToVertex(Vector3f from, Vector3f to, int index, ForgeDirection side) {
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

        final Matrix4f vRot = data.getAffineMatrix();
        final var sidedQuadStore = new HashMap<ModelQuadFacing, ArrayList<ModelQuadView>>(7);

        // Append faces from each element
        for (ModelDeserializer.ModelElement e : this.elements) {

            final Matrix4f rot = (e.rotation() == null) ? NOOP.getAffineMatrix() : e.rotation().getAffineMatrix();

            final Vector3f from = e.from();
            final Vector3f to = e.to();

            for (Face f : e.faces()) {

                float x = Float.MAX_VALUE;
                float y = Float.MAX_VALUE;
                float z = Float.MAX_VALUE;
                float X = Float.MIN_VALUE;
                float Y = Float.MIN_VALUE;
                float Z = Float.MIN_VALUE;

                // Assign vertexes
                final var quad = new ModelQuad();
                for (int i = 0; i < 4; ++i) {

                    final Vector3f vert = mapSideToVertex(from, to, i, f.name()).mulPosition(rot).mulPosition(vRot);
                    quad.setX(i, vert.x);
                    quad.setY(i, vert.y);
                    quad.setZ(i, vert.z);

                    x = min(x, vert.x);
                    y = min(y, vert.y);
                    z = min(z, vert.z);
                    X = max(X, vert.x);
                    Y = max(Y, vert.y);
                    Z = max(Z, vert.z);
                }

                // Set culling and nominal faces
                final var normFace = quad.getNormalFace();
                quad.setLightFace(normFace != UNASSIGNED ? normFace : POS_Y);

                // Set UV
                // TODO: UV locking
                final Vector4f uv = Objects.firstNonNull(f.uv(), DEFAULT_UV);
                setUV(quad, 0, uv.x, uv.y);
                setUV(quad, 1, uv.x, uv.w);
                setUV(quad, 2, uv.z, uv.w);
                setUV(quad, 3, uv.z, uv.y);

                // Set the sprite
                bakeSprite(quad, this.textures.get(f.texture()));

                // Set the tint index
                quad.setColorIndex(f.tintIndex());

                // Set AO
                quad.setHasAmbientOcclusion(this.useAO);

                // Bake and add it
                sidedQuadStore.computeIfAbsent(quad.getNormalFace(), d -> new ArrayList<>()).add(quad);
            }
        }

        // Add them to the model
        return new PileOfQuads(sidedQuadStore);
    }

    // TODO fix
    private void bakeSprite(ModelQuadViewMutable quad, String name) {
        final var icon = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
        final float minU = icon.getMinU();
        final float minV = icon.getMinV();
        final float dU = icon.getMaxU() - minU;
        final float dV = icon.getMaxV() - minV;

        for (int i = 0; i < 4; ++i) {
            quad.setTexU(i, fma(dU, quad.getTexU(i) / 16, minU));
            quad.setTexV(i, fma(dV, quad.getTexV(i) / 16, minV));
        }
    }

    public void resolveParents(Function<ModelLocation, JSONModel> modelLoader) {

        if (this.parentId != null && this.parent == null) {

            final JSONModel p = modelLoader.apply(this.parentId);
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
}
