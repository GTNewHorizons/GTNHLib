package com.gtnewhorizon.gtnhlib.client.model.json;

import static com.gtnewhorizon.gtnhlib.client.renderer.util.DirectionUtil.ALL_DIRECTIONS;
import static java.lang.Math.max;
import static java.lang.Math.min;

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

import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.impl.NdQuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.BakedModel;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.Quad;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;

import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.Getter;

public class JsonModel implements BakedModel {

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

    public void bake(JSONVariant v) {

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

}
