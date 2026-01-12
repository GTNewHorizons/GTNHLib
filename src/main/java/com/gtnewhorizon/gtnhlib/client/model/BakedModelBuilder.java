package com.gtnewhorizon.gtnhlib.client.model;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Vec3;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.CapturingTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

/**
 * A class that converts multiple quads with different rotations/translations/scales into 1 VBO. This has the benefit of
 * reduced matrix transformations and reduced render calls. This class has the same methods as ModelRenderer, making it
 * easy to convert existing ModelRenderer code.
 * <p>
 * Keep in mind that this class does NOT work on objects that use a non-constant transformation for its parts.
 */
public class BakedModelBuilder {

    private float textureWidth;
    private float textureHeight;
    private int textureOffsetX;
    private int textureOffsetY;
    private float rotationPointX;
    private float rotationPointY;
    private float rotationPointZ;
    // Note: These are in radians, not degrees. Same as ModelRenderer
    private float rotateAngleX;
    private float rotateAngleY;
    private float rotateAngleZ;
    private boolean mirror;
    private final DirectTessellator tessellator;

    private final Matrix4f modelMatrix = new Matrix4f();

    public BakedModelBuilder(int textureWidth, int textureHeight) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;

        this.tessellator = TessellatorManager.startCapturingDirect();
        tessellator.startDrawing(GL11.GL_QUADS);
    }

    public BakedModelBuilder(ModelBase model) {
        this(model.textureWidth, model.textureHeight);
    }

    public BakedModelBuilder(ModelBase model, int textureOffsetX, int textureOffsetY) {
        this(model);
        this.setTextureOffset(textureOffsetX, textureOffsetY);
    }

    public BakedModelBuilder setTextureOffset(int textureOffsetX, int textureOffsetY) {
        this.textureOffsetX = textureOffsetX;
        this.textureOffsetY = textureOffsetY;
        return this;
    }

    public BakedModelBuilder setTextureSize(int textureWidth, int textureHeight) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        return this;
    }

    public BakedModelBuilder setRotationPoint(float rotationPointX, float rotationPointY, float rotationPointZ) {
        this.rotationPointX = rotationPointX;
        this.rotationPointY = rotationPointY;
        this.rotationPointZ = rotationPointZ;
        return this;
    }

    public BakedModelBuilder setRotationAngles(float x, float y, float z) {
        this.rotateAngleX = x;
        this.rotateAngleY = y;
        this.rotateAngleZ = z;
        return this;
    }

    public BakedModelBuilder setRotationAngleX(float x) {
        this.rotateAngleX = x;
        return this;
    }

    public BakedModelBuilder setRotationAngleY(float y) {
        this.rotateAngleY = y;
        return this;
    }

    public BakedModelBuilder setRotationAngleZ(float z) {
        this.rotateAngleZ = z;
        return this;
    }

    public BakedModelBuilder setMirrored() {
        this.mirror = true;
        return this;
    }

    public BakedModelBuilder setMirrored(boolean mirror) {
        this.mirror = mirror;
        return this;
    }

    public void addBoxVertices(float x1, float y1, float z1, int xWidth, int yWidth, int zWidth, float scale) {
        addBoxVertices(x1, y1, z1, xWidth, yWidth, zWidth, getModelMatrix(scale), scale);
    }

    public void addBoxVertices(float x1, float y1, float z1, int xWidth, int yWidth, int zWidth, Matrix4f mat4f,
            float scale) {
        new ModelBox(this, this.textureOffsetX, this.textureOffsetY, x1, y1, z1, xWidth, yWidth, zWidth)
                .addVerticesToTesselator(tessellator, mat4f, scale);
    }

    public Matrix4f getModelMatrix(float scale) {
        final Matrix4f mat4f = modelMatrix.identity();

        mat4f.translate(this.rotationPointX * scale, this.rotationPointY * scale, this.rotationPointZ * scale);

        if (this.rotateAngleZ != 0.0F) {
            mat4f.rotate(this.rotateAngleZ, 0.0F, 0.0F, 1.0F);
        }
        if (this.rotateAngleY != 0.0F) {
            mat4f.rotate(this.rotateAngleY, 0.0F, 1.0F, 0.0F);
        }
        if (this.rotateAngleX != 0.0F) {
            mat4f.rotate(this.rotateAngleX, 1.0F, 0.0F, 0.0F);
        }
        return mat4f;
    }

    @Deprecated
    public VertexBuffer finish(VertexFormat format) {
        return (VertexBuffer) tessellator.stopCapturingToVBO(VertexBufferType.MUTABLE_RESIZABLE);
    }

    public IVertexBuffer finish() {
        return tessellator.stopCapturingToVBO(VertexBufferType.IMMUTABLE);
    }

    // spotless:off
    // Mostly copied from net.minecraft.client.model.ModelBox + remapped
    private static class ModelBox {
        private final TexturedQuad[] quadList;

        public ModelBox(
            BakedModelBuilder builder,
            int texOffsetX, int texOffsetY,
            float minX, float minY, float minZ,
            int xWidth, int yWidth, int zWidth
        ) {
            this.quadList = new TexturedQuad[6];
            float maxX = minX + (float) xWidth;
            float maxY = minY + (float) yWidth;
            float maxZ = minZ + (float) zWidth;

            if (builder.mirror) {
                float temp = maxX;
                maxX = minX;
                minX = temp;
            }

            PositionTextureVertex positiontexturevertex7 = new PositionTextureVertex(
                minX, minY, minZ,
                0.0F, 0.0F
            );
            PositionTextureVertex positiontexturevertex = new PositionTextureVertex(
                maxX, minY, minZ,
                0.0F, 8.0F
            );
            PositionTextureVertex positiontexturevertex1 = new PositionTextureVertex(
                maxX, maxY, minZ,
                8.0F, 8.0F
            );
            PositionTextureVertex positiontexturevertex2 = new PositionTextureVertex(
                minX, maxY, minZ,
                8.0F, 0.0F
            );
            PositionTextureVertex positiontexturevertex3 = new PositionTextureVertex(
                minX, minY, maxZ,
                0.0F, 0.0F
            );
            PositionTextureVertex positiontexturevertex4 = new PositionTextureVertex(
                maxX, minY, maxZ,
                0.0F, 8.0F
            );
            PositionTextureVertex positiontexturevertex5 = new PositionTextureVertex(
                maxX, maxY, maxZ,
                8.0F, 8.0F
            );
            PositionTextureVertex positiontexturevertex6 = new PositionTextureVertex(
                minX, maxY, maxZ,
                8.0F, 0.0F
            );
            this.quadList[0] = new TexturedQuad(
                new PositionTextureVertex[] {positiontexturevertex4, positiontexturevertex, positiontexturevertex1, positiontexturevertex5},
                texOffsetX + zWidth + xWidth,
                texOffsetY + zWidth,
                texOffsetX + zWidth + xWidth + zWidth,
                texOffsetY + zWidth + yWidth,
                builder.textureWidth,
                builder.textureHeight
            );
            this.quadList[1] = new TexturedQuad(
                new PositionTextureVertex[] {positiontexturevertex7, positiontexturevertex3, positiontexturevertex6, positiontexturevertex2},
                texOffsetX,
                texOffsetY + zWidth,
                texOffsetX + zWidth,
                texOffsetY + zWidth + yWidth,
                builder.textureWidth,
                builder.textureHeight
            );
            this.quadList[2] = new TexturedQuad(
                new PositionTextureVertex[] {positiontexturevertex4, positiontexturevertex3, positiontexturevertex7, positiontexturevertex},
                texOffsetX + zWidth,
                texOffsetY,
                texOffsetX + zWidth + xWidth,
                texOffsetY + zWidth,
                builder.textureWidth,
                builder.textureHeight
            );
            this.quadList[3] = new TexturedQuad(
                new PositionTextureVertex[] {positiontexturevertex1, positiontexturevertex2, positiontexturevertex6, positiontexturevertex5},
                texOffsetX + zWidth + xWidth,
                texOffsetY + zWidth,
                texOffsetX + zWidth + xWidth + xWidth,
                texOffsetY,
                builder.textureWidth,
                builder.textureHeight
            );
            this.quadList[4] = new TexturedQuad(
                new PositionTextureVertex[] {positiontexturevertex, positiontexturevertex7, positiontexturevertex2, positiontexturevertex1},
                texOffsetX + zWidth,
                texOffsetY + zWidth,
                texOffsetX + zWidth + yWidth,
                texOffsetY + zWidth + yWidth,
                builder.textureWidth,
                builder.textureHeight
            );
            this.quadList[5] = new TexturedQuad(
                new PositionTextureVertex[] {positiontexturevertex3, positiontexturevertex4, positiontexturevertex5, positiontexturevertex6},
                texOffsetX + zWidth + yWidth + zWidth,
                texOffsetY + zWidth,
                texOffsetX + zWidth + yWidth + zWidth + yWidth,
                texOffsetY + zWidth + yWidth,
                builder.textureWidth,
                builder.textureHeight
            );

            if (builder.mirror) {
                for (TexturedQuad texturedQuad : this.quadList) {
                    texturedQuad.flipFace();
                }
            }
        }

        public void addVerticesToTesselator(
            DirectTessellator tessellator,
            Matrix4f mat4f,
            float scale
        ) {
            // These 2 are reuseable vectors
            Vector3f vec3f = new Vector3f();
            Vector4f vec4f = new Vector4f();
            Matrix3f normalMat = NormalHelper.getNormalMatrix(mat4f);
            for (TexturedQuad quad : this.quadList) {
                PositionTextureVertex[] vertices = quad.vertexPositions;
                final Vec3 v1 = vertices[1].vector3D;
                Vec3 vec3 = v1.subtract(vertices[0].vector3D);
                Vec3 vec31 = v1.subtract(vertices[2].vector3D);
                Vec3 cross = vec31.crossProduct(vec3);

                vec3f.x = (float) cross.xCoord;
                vec3f.y = (float) cross.yCoord;
                vec3f.z = (float) cross.zCoord;
                NormalHelper.setNormalTransformed(tessellator, vec3f, normalMat);
                // Reverse so it works with culling
                for (int i = 3; i >= 0; i--) {
                    PositionTextureVertex vertex = quad.vertexPositions[i];
                    addVertexWithUV(
                        tessellator, vec4f, mat4f,
                        (float) (vertex.vector3D.xCoord * scale),
                        (float) (vertex.vector3D.yCoord * scale),
                        (float) (vertex.vector3D.zCoord * scale),
                        vertex.texturePositionX, vertex.texturePositionY
                    );
                }
            }
        }

        private static void addVertexWithUV(
            Tessellator tessellator,
            Vector4f vec, Matrix4f mat4f,
            float x, float y, float z,
            float u, float v
        ) {
            vec.x = x;
            vec.y = y;
            vec.z = z;
            vec.w = 1;
            vec.mul(mat4f);
            tessellator.addVertexWithUV(vec.x, vec.y, vec.z, u, v);
        }
    }
    //spotless:on
}
