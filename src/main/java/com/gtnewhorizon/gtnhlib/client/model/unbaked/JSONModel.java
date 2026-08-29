package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import static com.gtnewhorizon.gtnhlib.client.model.JSONVariant.DEG2RAD;
import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;
import static com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing.NEG_Y;
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

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

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

    private static final Map<ForgeDirection, Matrix4f> UV_TRANSFORM_LOCAL_TO_GLOBAL =
        new EnumMap<>(ForgeDirection.class);

    private static final Map<ForgeDirection, Matrix4f> UV_TRANSFORM_GLOBAL_TO_LOCAL =
        new EnumMap<>(ForgeDirection.class);

    // Get these magic matrices
    static {
        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(
            ForgeDirection.SOUTH,
            new Matrix4f()
        );

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(
            ForgeDirection.EAST,
            new Matrix4f().rotateY((float) (Math.PI / 2))
        );

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(
            ForgeDirection.WEST,
            new Matrix4f().rotateY((float) (-Math.PI / 2))
        );

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(
            ForgeDirection.NORTH,
            new Matrix4f().rotateY((float) Math.PI)
        );

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(
            ForgeDirection.UP,
            new Matrix4f().rotateX((float) (-Math.PI / 2))
        );

        UV_TRANSFORM_LOCAL_TO_GLOBAL.put(
            ForgeDirection.DOWN,
            new Matrix4f().rotateX((float) (Math.PI / 2))
        );

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            UV_TRANSFORM_GLOBAL_TO_LOCAL.put(
                direction,
                new Matrix4f(
                    UV_TRANSFORM_LOCAL_TO_GLOBAL.get(direction)
                ).invert()
            );
        }
    }


    private static final float RESCALE_22_5 = 1.0F / (float)Math.cos((float) (Math.PI / 8)) - 1.0F;
    private static final float RESCALE_45 = 1.0F / (float)Math.cos((float) (Math.PI / 4)) - 1.0F;

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

    // Probably possible to marry the logic with what is above, but this is likely the fastest impl.
    private static BakedUV calculateDefaultUV(
        Vector3f from,
        Vector3f to,
        ForgeDirection face,
        int rotation)
    {
        return switch (face)
        {
            case DOWN -> new BakedUV(
                new Vector4f(
                    from.x,
                    from.z,
                    to.x,
                    to.z
                ),
                rotation
            );

            case UP -> new BakedUV(
                new Vector4f(
                    from.x,
                    16.0F - to.z,
                    to.x,
                    16.0F - from.z
                ),
                rotation
            );

            case NORTH -> new BakedUV(
                new Vector4f(
                    16.0F - to.x,
                    16.0F - to.y,
                    16.0F - from.x,
                    16.0F - from.y
                ),
                rotation
            );

            case SOUTH -> new BakedUV(
                new Vector4f(
                    from.x,
                    16.0F - to.y,
                    to.x,
                    16.0F - from.y
                ),
                rotation
            );

            case WEST -> new BakedUV(
                new Vector4f(
                    from.z,
                    16.0F - to.y,
                    to.z,
                    16.0F - from.y
                ),
                rotation
            );

            case EAST -> new BakedUV(
                new Vector4f(
                    16.0F - to.z,
                    16.0F - to.y,
                    16.0F - from.z,
                    16.0F - from.y
                ),
                rotation
            );

            default -> throw new IllegalArgumentException(
                "Unsupported face: " + face
            );
        };
    }

    @Override
    public BakedModel bake(BakeData data) {

        final var vRot = data.getAffineMatrix();
        final var sidedQuadStore = new HashMap<ModelQuadFacing, ArrayList<ModelQuadView>>(7);

        // Append faces from each element
        for (ModelDeserializer.ModelElement e : this.elements) {

            final Vector3f from = e.from();
            final Vector3f to = e.to();

            for (Face f : e.faces()) {

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
                texName = texName.replaceFirst("^minecraft:", "");
                final TextureAtlasSprite icon = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texName);

                // Assign vertexes
                final var quad = new ModelQuad();
                quad.setSprite(icon);

                // Set shading properties
                quad.setEmissiveness(e.lightEmission());
                quad.setDirectionalShading(e.shade());
                quad.setHasAmbientOcclusion(this.useAO);

                // Figure out the UV rotations
                BakedUV bakedUV;
                if (f.bakedUV().uv == null)
                {
                    bakedUV = calculateDefaultUV(from, to, f.name(), f.bakedUV().rotation());
                }
                else
                {
                    bakedUV = f.bakedUV();
                }

                // If uv is locked we need to recompute the UVs based on the rotation of the element
                // and the model rotation
                if (data.uvLock()) {
                    bakedUV = recomputeUVs(
                        f.bakedUV(),
                        f.name(),
                        new Matrix4f(vRot)
                    );
                }

                // Bake vertexes
                for (int i = 0; i < 4; i++)
                {
                    bakeVertex(quad, i, from, to, f.name(), e.rotation(), vRot, bakedUV, icon);
                }

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
    // just to keep it simpler. I've modified them A LITTLE, and have documented them because matrix/vector
    // math is really fucking hard to read unless you're some super genius I guess.


    private static void bakeVertex(ModelQuadViewMutable quad, int vertexIndex, Vector3f from, Vector3f to, ForgeDirection face, ModelDeserializer.ModelElement.Rotation elementRotation, Matrix4fc modelRotation, BakedUV uv, TextureAtlasSprite icon) {
        Vector3f vert = mapSideToVertex(from, to, vertexIndex, face);
        applyElementRotation(vert, elementRotation);
        applyModelRotation(vert, modelRotation);
        fillVertex(quad, vertexIndex, vert, icon, uv);
    }

    /**
     * Sets the vertexes of a quad given an index. Also bakes the sprite in.
     *
     * @param quad the quad to fill
     * @param index index of the vert in the quad.
     * @param position which vertex to fill in for the quad. This is computed beforehand to match the vertex
     * @param sprite the sprite to bake
     * @param uv the UVs of the sprite
     */
    private static void fillVertex(ModelQuadViewMutable quad, int index, Vector3f position, TextureAtlasSprite sprite, BakedUV uv)
    {
        quad.setX(index, position.x());
        quad.setY(index, position.y());
        quad.setZ(index, position.z());
        quad.setColor(index, -1); // Needed?
        quad.setTexU(
            index,
            sprite.getInterpolatedU(uv.getU(index))
        );
        quad.setTexV(
            index,
            sprite.getInterpolatedV(uv.getV(index))
        );
    }

    /**
     * Applies model rotation to the matrix given a model rotation matrix argument
     *
     * @param vertex the vertex to rotate
     * @param rotation the rotation to apply
     */
    private static void applyModelRotation(Vector3f vertex, Matrix4fc rotation) {
        if (!rotation.equals(new Matrix4f(), 1.0e-6f)) { // Check for identity
            transformVertex(
                vertex,
                new Vector3f(0.5F, 0.5F, 0.5F),
                rotation,
                new Vector3f(1.0F, 1.0F, 1.0F)
            );
        }
    }

    /**
     * Applies element rotation to the matrix given a {@link ModelDeserializer.ModelElement.Rotation} argument
     *
     * @param vertex the vertex to rotate
     * @param rotation the rotation to apply
     */
    private static void applyElementRotation(Vector3f vertex, @Nullable ModelDeserializer.ModelElement.Rotation rotation)
    {
        if (rotation == null) {
            return;
        }

        Vector3f axis;
        Vector3f rescale = switch (rotation.axis()) {
            case X -> {
                axis = new Vector3f(1.0F, 0.0F, 0.0F);
                yield new Vector3f(0.0F, 1.0F, 1.0F);
            }
            case Y -> {
                axis = new Vector3f(0.0F, 1.0F, 0.0F);
                yield new Vector3f(1.0F, 0.0F, 1.0F);
            }
            case Z -> {
                axis = new Vector3f(0.0F, 0.0F, 1.0F);
                yield new Vector3f(1.0F, 1.0F, 0.0F);
            }
            default -> throw new IllegalArgumentException("There are only 3 axes");
        };

        Quaternionf quaternion = new Quaternionf().rotationAxis(rotation.angle() * DEG2RAD, axis);

        if (rotation.rescale())
        {
            if (Math.abs(rotation.angle()) == 22.5F)
            {
                rescale.mul(RESCALE_22_5);
            }
            else
            {
                rescale.mul(RESCALE_45);
            }

            rescale.add(1.0F, 1.0F, 1.0F);
        }
        else
        {
            rescale.set(1.0F, 1.0F, 1.0F);
        }

        transformVertex(
            vertex,
            rotation.origin(),
            new Matrix4f().rotation(quaternion),
            rescale
        );
    }

    /**
     * Applies a rotation and rescaling to a vertex around a specified origin.
     * <p>The vertex is translated relative to the origin, transformed by the rotation
     * matrix, scaled by the supplied scale factor, and then translated back.</p>
     *
     * @param vertex the vertex to transform; modified in place
     * @param origin the point around which the transformation is applied
     * @param rotation the rotation matrix to apply
     * @param scale the scale factor to apply after rotation
     */
    private static void transformVertex(Vector3f vertex, Vector3f origin, Matrix4fc rotation, Vector3f scale) {
        Vector4f vector4f = rotation.transform(new Vector4f(vertex.x() - origin.x(), vertex.y() - origin.y(), vertex.z() - origin.z(), 1.0F));
        vector4f.mul(new Vector4f(scale, 1.0F));
        vertex.set(vector4f.x() + origin.x(), vector4f.y() + origin.y(), vector4f.z() + origin.z());
    }

    /**
     * Recomputes the UV based on the model rotation and the uv lock status.
     *
     * @param bakedUV the uv to recompute
     * @param face the face it belongs to
     * @param modelRotation the rotation of the model
     * @return a new recomputed bakedUV for uvlock
     */
    private static BakedUV recomputeUVs(BakedUV bakedUV, ForgeDirection face, Matrix4fc modelRotation)
    {
        Matrix4f uvLockTransform = getUVLockTransform(modelRotation, face);

        float minU = bakedUV.getU(bakedUV.getReverseIndex(0));
        float minV = bakedUV.getV(bakedUV.getReverseIndex(0));

        Vector4f transformedFirstUV = uvLockTransform.transform(
            new Vector4f(minU / 16.0F, minV / 16.0F, 0.0F, 1.0F)
        );

        float transformedMinU = 16.0F * transformedFirstUV.x();
        float transformedMinV = 16.0F * transformedFirstUV.y();

        float maxU = bakedUV.getU(bakedUV.getReverseIndex(2));
        float maxV = bakedUV.getV(bakedUV.getReverseIndex(2));

        Vector4f transformedSecondUV = uvLockTransform.transform(
            new Vector4f(maxU / 16.0F, maxV / 16.0F, 0.0F, 1.0F)
        );

        float transformedMaxU = 16.0F * transformedSecondUV.x();
        float transformedMaxV = 16.0F * transformedSecondUV.y();

        float u0;
        float u1;
        if (Math.signum(maxU - minU) == Math.signum(transformedMaxU - transformedMinU)) {
            u0 = transformedMinU;
            u1 = transformedMaxU;
        } else {
            u0 = transformedMaxU;
            u1 = transformedMinU;
        }

        float v0;
        float v1;
        if (Math.signum(maxV - minV) == Math.signum(transformedMaxV - transformedMinV)) {
            v0 = transformedMinV;
            v1 = transformedMaxV;
        } else {
            v0 = transformedMaxV;
            v1 = transformedMinV;
        }

        float rotationRadians = (float) Math.toRadians(bakedUV.rotation);

        Matrix3f rotationMatrix = new Matrix3f(uvLockTransform);
        Vector3f transformedRotation = rotationMatrix.transform(
            new Vector3f(
                MathHelper.cos(rotationRadians),
                MathHelper.sin(rotationRadians),
                0.0F
            )
        );

        int rotation = Math.floorMod(
            -((int) Math.round(
                Math.toDegrees(
                    Math.atan2(transformedRotation.y(), transformedRotation.x()
                    )
                ) / 90.0
            )) * 90,
            360
        );
        return new BakedUV(new Vector4f(u0, v0, u1, v1), rotation);
    }

    /**
     * Gets the uvlock transformation for a given face and model rotation
     *
     * @param modelRotation get the transformation matrix based on the model rotation when UV lock is applied
     * @param face face to do the transform from
     * @return a matrix that represents the required rotation for uv lock and the given face
     */
    private static Matrix4f getUVLockTransform(
        Matrix4fc modelRotation,
        ForgeDirection face) {

        ForgeDirection rotatedFace = rotate(modelRotation, face);


        Matrix4f inverse = inverseOrIdentity(modelRotation);

        Matrix4f result = new Matrix4f(
            UV_TRANSFORM_GLOBAL_TO_LOCAL.get(face)
        );

        result.mul(inverse);
        result.mul(UV_TRANSFORM_LOCAL_TO_GLOBAL.get(rotatedFace));

        return blockCenterToCorner(result);
    }

    /**
     * Gets the inverse of the input matrix if it's non-singular, otherwise gets the identity matrix.
     *
     * @param matrix matrix to get the inverse of
     * @return The inverse of the input matrix or the identity if it's singular.
     */
    private static Matrix4f inverseOrIdentity(Matrix4fc matrix) {
        if (Math.abs(matrix.determinant()) < 1.0e-6f)
        {
            // pretty sure this can only happen if the scaling values for a given
            // model are fucked up to have a 0 width
            GTNHLib.error("Failed to invert the UV transformation! Likely something wrong with scaling values.");
            return new Matrix4f();
        }

        return new Matrix4f(matrix).invert();
    }

    /**
     * Returns a matrix that is translates something to the corner from the block center
     *
     * @param transform input transformation
     * @return the input transformation with a translation to the corner from the center of a block
     */
    private static Matrix4f blockCenterToCorner(Matrix4f transform) {
        Matrix4f matrix = new Matrix4f()
            .translation(0.5f, 0.5f, 0.5f);

        matrix.mul(transform);
        matrix.translate(-0.5f, -0.5f, -0.5f);

        return matrix;
    }

    /**
     * Rotates a direction using the supplied transformation matrix and returns
     * the nearest cardinal direction to the transformed result.
     *
     * @param matrix the transformation matrix used to rotate the direction
     * @param direction the direction to rotate
     * @return the cardinal direction nearest to the transformed direction
     */
    private static ForgeDirection rotate(
        Matrix4fc matrix,
        ForgeDirection direction) {

        Vector3f normal = directionVector(direction);

        Vector4f transformed = matrix.transform(
            new Vector4f(normal.x, normal.y, normal.z, 0.0f)
        );

        return getApproximateNearest(
            transformed.x,
            transformed.y,
            transformed.z
        );
    }

    /**
     * Gets the nearest forge direction given x y z.
     */
    private static ForgeDirection getApproximateNearest(
        float x,
        float y,
        float z) {

        ForgeDirection closest = ForgeDirection.NORTH;
        float best = -Float.MAX_VALUE;

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            Vector3f normal = directionVector(direction);

            float dot =
                x * normal.x +
                    y * normal.y +
                    z * normal.z;

            if (dot > best) {
                best = dot;
                closest = direction;
            }
        }

        return closest;
    }

    /**
     * Given a forge direction give the direction vector in that direction
     */
    private static Vector3f directionVector(ForgeDirection direction) {
        return new Vector3f(
            direction.offsetX,
            direction.offsetY,
            direction.offsetZ
        );
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
    public record BakedUV(Vector4f uv, int rotation)
    {
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
