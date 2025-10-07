package com.gtnewhorizon.gtnhlib.client.model.loading;

import com.gtnewhorizon.gtnhlib.client.renderer.quad.Quad;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadBuilder;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.properties.ModelQuadFlags;
import net.minecraftforge.common.util.ForgeDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class NdQuadBuilder extends Quad implements QuadBuilder {

    private ForgeDirection nominalFace = ForgeDirection.UNKNOWN;
    // Defaults to UP, but only because it can't be UNKNOWN or null
    private ForgeDirection lightFace = ForgeDirection.UP;
    private int geometryFlags = 0;
    private boolean isGeometryInvalid = true;
    final Vector3f faceNormal = new Vector3f();
    public final Material mat = new Material();

    private void computeGeometry() {
        if (this.isGeometryInvalid) {
            this.isGeometryInvalid = false;

            NormalHelper.computeFaceNormal(this.faceNormal, this);

            // depends on face normal
            this.lightFace = GeometryHelper.lightFace(this);

            // depends on light face
            this.geometryFlags = ModelQuadFlags.getQuadFlags(this);
        }
    }

    @Override
    public boolean isShade() {
        return this.mat.getDiffuse();
    }

    @Override
    @NotNull
    public ForgeDirection getLightFace() {
        this.computeGeometry();
        return this.lightFace;
    }

    @Override
    public void setCullFace(ForgeDirection dir) {
        super.setCullFace(dir);
        this.nominalFace(dir);
    }

    @Override
    public void nominalFace(@Nullable ForgeDirection face) {
        this.nominalFace = face;
    }

    @Override
    public ForgeDirection nominalFace() {
        return this.nominalFace;
    }

    @Override
    public float posByIndex(int vertexIndex, int coordinateIndex) {
        return Float.intBitsToFloat(this.data[vertexIndex * Quad.VERTEX_STRIDE + Quad.X_INDEX + coordinateIndex]);
    }

    @Override
    public int getFlags() {
        return this.geometryFlags;
    }

}
