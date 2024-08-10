package com.gtnewhorizon.gtnhlib.mixins.early;

import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.client.model.obj.Face;
import net.minecraftforge.client.model.obj.TextureCoordinate;
import net.minecraftforge.client.model.obj.Vertex;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.gtnhlib.client.renderer.FaceBehaviorManager;

@Mixin(value = Face.class, remap = false)
public abstract class MixinFace {

    @Shadow
    private Vertex[] vertices;

    @Shadow
    private Vertex[] vertexNormals;

    @Shadow
    private Vertex faceNormal;

    @Shadow
    private TextureCoordinate[] textureCoordinates;

    @Shadow
    public abstract Vertex calculateFaceNormal();

    @Inject(method = "addFaceForRender(Lnet/minecraft/client/renderer/Tessellator;F)V", at = @At("HEAD"), cancellable = true)
    private void onAddFaceForRender(Tessellator tessellator, float textureOffset, CallbackInfo ci) {
        System.out.println("Hijack");
        if (FaceBehaviorManager.getVertexNormalBehavior()) {
            if (faceNormal == null) {
                faceNormal = this.calculateFaceNormal();
            }
            tessellator.setNormal(faceNormal.x, faceNormal.y, faceNormal.z);

            float averageU = 0F;
            float averageV = 0F;

            if ((textureCoordinates != null) && (textureCoordinates.length > 0)) {
                for (int i = 0; i < textureCoordinates.length; ++i) {
                    averageU += textureCoordinates[i].u;
                    averageV += textureCoordinates[i].v;
                }

                averageU = averageU / textureCoordinates.length;
                averageV = averageV / textureCoordinates.length;
            }

            float offsetU, offsetV;

            for (int i = 0; i < vertices.length; ++i) {
                if ((vertexNormals != null) && (vertexNormals.length > 0)) {
                    System.out.println("PRINTING");
                    tessellator.setNormal(vertexNormals[i].x, vertexNormals[i].y, vertexNormals[i].z);
                }

                if ((textureCoordinates != null) && (textureCoordinates.length > 0)) {
                    offsetU = textureOffset;
                    offsetV = textureOffset;

                    if (textureCoordinates[i].u > averageU) {
                        offsetU = -offsetU;
                    }
                    if (textureCoordinates[i].v > averageV) {
                        offsetV = -offsetV;
                    }

                    tessellator.addVertexWithUV(
                            vertices[i].x,
                            vertices[i].y,
                            vertices[i].z,
                            textureCoordinates[i].u + offsetU,
                            textureCoordinates[i].v + offsetV);
                } else {
                    tessellator.addVertex(vertices[i].x, vertices[i].y, vertices[i].z);
                }
            }

            ci.cancel();
        }
    }
}
