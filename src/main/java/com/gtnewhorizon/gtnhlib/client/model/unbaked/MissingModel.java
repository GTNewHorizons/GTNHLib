package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.PileOfQuads;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;

public class MissingModel extends JSONModel {

    public static final MissingModel MISSING_MODEL = new MissingModel();

    public MissingModel() {
        super(null, false, null, Object2ObjectMaps.emptyMap(), null);
    }

    @Override
    public BakedModel bake() {
        return super.bake();
    }

    /**
     * Overrides the default baking logic to bake a hardcoded cube. Used when a state not specified in the blockstate
     * file is chosen to render. Also appears when the model provided is missing.
     * <p>
     * TODO: Possible memory leak since a new one is baked for every missing state. Perhaps we could store a singleton
     * that's cached on first bake, that is reset on resource reload. Would that need a threadlocal?
     */
    @Override
    public BakedModel bake(BakeData data) {
        final var vRot = data.getAffineMatrix();
        final var sidedQuadStore = new HashMap<ModelQuadFacing, ArrayList<ModelQuadView>>(7);
        for (ForgeDirection facing : ForgeDirection.VALID_DIRECTIONS) {
            ModelQuadFacing quadFacing = ModelQuadFacing.fromForgeDir(facing);
            final Matrix4f rot = NOOP.getAffineMatrix();

            // The from vector is the minimum corner of the face's element
            final Vector3f from = switch (quadFacing) {
                case POS_X -> new Vector3f(1, 0, 0);
                case POS_Y -> new Vector3f(0, 1, 0);
                case POS_Z -> new Vector3f(0, 0, 1);
                case NEG_X, NEG_Y, NEG_Z -> new Vector3f(0, 0, 0);
                case UNASSIGNED -> null;
            };

            // The to vector is the maximum corner of the face's element
            final Vector3f to = switch (quadFacing) {
                // POS_X (Fixed X=1, Y/Z span 0->1)
                case POS_X, POS_Y, POS_Z -> new Vector3f(1, 1, 1);
                case NEG_X -> new Vector3f(0, 1, 1);
                case NEG_Y -> new Vector3f(1, 0, 1);
                case NEG_Z -> new Vector3f(1, 1, 0);
                case UNASSIGNED -> null;
            };

            // Assign vertexes
            final var quad = new ModelQuad();
            for (int i = 0; i < 4; ++i) {
                final Vector3f vert = mapSideToVertex(from, to, i, facing).mulPosition(rot).mulPosition(vRot);
                quad.setX(i, vert.x);
                quad.setY(i, vert.y);
                quad.setZ(i, vert.z);
            }

            // Set culling and nominal faces
            quad.setLightFace(quadFacing);
            quad.setColorIndex(-1);
            quad.setDirectionalShading(true);
            quad.setEmissiveness(0);

            // Set UV
            final Vector4f uv = DEFAULT_UV;
            setUV(quad, 0, uv.x, uv.y);
            setUV(quad, 1, uv.x, uv.w);
            setUV(quad, 2, uv.z, uv.w);
            setUV(quad, 3, uv.z, uv.y);

            bakeSprite(quad, "missingno");

            // Bake and add it
            sidedQuadStore.computeIfAbsent(quadFacing, d -> new ArrayList<>()).add(quad);
        }

        final IIcon missing = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("missingno");
        // Add them to the model
        return new PileOfQuads(sidedQuadStore, Collections.emptyMap(), missing);
    }
}
