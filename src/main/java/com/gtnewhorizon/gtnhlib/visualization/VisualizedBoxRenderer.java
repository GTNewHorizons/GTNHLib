package com.gtnewhorizon.gtnhlib.visualization;

import java.nio.ByteBuffer;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.joml.primitives.AABBfc;
import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.DefaultVertexFormat;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@EventBusSubscriber(side = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class VisualizedBoxRenderer {

    private static final VertexBuffer VBO = new VertexBuffer(DefaultVertexFormat.POSITION_COLOR_TEXTURE, GL11.GL_QUADS);

    private static long timeout;
    private static boolean disableDepth;
    private static List<VisualizedBox> boxes;

    public static void receiveBoxes(long timeout, boolean append, List<VisualizedBox> boxes, boolean disableDepth) {
        VisualizedBoxRenderer.timeout = System.currentTimeMillis() + timeout;
        VisualizedBoxRenderer.disableDepth = disableDepth;

        if (append) {
            VisualizedBoxRenderer.boxes.addAll(boxes);
        } else {
            VisualizedBoxRenderer.boxes = boxes;
        }
    }

    @SubscribeEvent
    public static void renderBoxes(RenderWorldLastEvent event) {
        if (boxes == null || boxes.isEmpty()) return;
        if (timeout < System.currentTimeMillis()) {
            boxes = null;
            return;
        }

        Entity player = Minecraft.getMinecraft().renderViewEntity;
        double xd = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double yd = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double zd = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;

        TessellatorManager.startCapturing();

        Tessellator tessellator = TessellatorManager.get();

        tessellator.startDrawingQuads();

        int i = 0;

        for (VisualizedBox box : boxes) {
            tessellator.setColorRGBA(box.color.getRed(), box.color.getGreen(), box.color.getBlue(), box.color.getAlpha());

            drawBox(tessellator, xd, yd, zd, box.bounds, i++);
        }

        tessellator.draw();

        ByteBuffer quads = TessellatorManager.stopCapturingToBuffer(DefaultVertexFormat.POSITION_COLOR_TEXTURE);

        QuadSorter.sortStandardFormat(DefaultVertexFormat.POSITION_COLOR_TEXTURE, quads.asFloatBuffer(), quads.capacity(), (float) xd, (float) yd, (float) zd);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);

        if (disableDepth) GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND); // enable blend so it is transparent
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        VBO.uploadStream(quads);
        VBO.render();

        GL11.glPopAttrib();
    }

    private static void drawBox(
        Tessellator tes,
        double eyeX,
        double eyeY,
        double eyeZ,
        AABBfc bounds,
        int index
    ) {
        double X = bounds.minX() - eyeX - index * 0.001;
        double Y = bounds.minY() - eyeY - index * 0.001;
        double Z = bounds.minZ() - eyeZ - index * 0.001;
        double worldX = bounds.minX();
        double worldY = bounds.minY();
        double worldZ = bounds.minZ();

        double sX = bounds.maxX() - bounds.minX() + index * 0.002;
        double sY = bounds.maxY() - bounds.minY() + index * 0.002;
        double sZ = bounds.maxZ() - bounds.minZ() + index * 0.002;

        // this rendering code is independently written by glee8e on July 10th, 2023
        // and is released as part of StructureLib under LGPL terms, just like everything else in this project
        // cube is a very special model. its facings can be rendered correctly by viewer distance without using
        // surface normals and view vector
        // here we do a 2 pass render.
        // first pass we draw obstructed faces (i.e. faces that are further away from player)
        // second pass we draw unobstructed faces
        for (int j = 0; j < 2; j++) {
            boolean unobstructedPass = j == 1;
            for (int i = 0; i < 6; i++) {
                switch (i) { // {DOWN, UP, NORTH, SOUTH, WEST, EAST}
                    case 0 -> {
                        // all these ifs is in form if ((is face unobstructed) != (is in unobstructred pass))
                        if (worldY >= eyeY != unobstructedPass) continue;
                        tes.setNormal(0, -1, 0);
                        tes.addVertex(X, Y, Z);
                        tes.addVertex(X + sX, Y, Z);
                        tes.addVertex(X + sX, Y, Z + sZ);
                        tes.addVertex(X, Y, Z + sZ);
                    }
                    case 1 -> {
                        if (worldY + sY <= eyeY != unobstructedPass) continue;
                        tes.setNormal(0, 1, 0);
                        tes.addVertex(X, Y + sY, Z);
                        tes.addVertex(X, Y + sY, Z + sZ);
                        tes.addVertex(X + sX, Y + sY, Z + sZ);
                        tes.addVertex(X + sX, Y + sY, Z);
                    }
                    case 2 -> {
                        if (worldZ >= eyeZ != unobstructedPass) continue;
                        tes.setNormal(0, 0, -1);
                        tes.addVertex(X, Y, Z);
                        tes.addVertex(X, Y + sY, Z);
                        tes.addVertex(X + sX, Y + sY, Z);
                        tes.addVertex(X + sX, Y, Z);
                    }
                    case 3 -> {
                        if (worldZ + sZ <= eyeZ != unobstructedPass) continue;
                        tes.setNormal(0, 0, 1);
                        tes.addVertex(X + sX, Y, Z + sZ);
                        tes.addVertex(X + sX, Y + sY, Z + sZ);
                        tes.addVertex(X, Y + sY, Z + sZ);
                        tes.addVertex(X, Y, Z + sZ);
                    }
                    case 4 -> {
                        if (worldX >= eyeX != unobstructedPass) continue;
                        tes.setNormal(-1, 0, 0);
                        tes.addVertex(X, Y, Z + sZ);
                        tes.addVertex(X, Y + sY, Z + sZ);
                        tes.addVertex(X, Y + sY, Z);
                        tes.addVertex(X, Y, Z);
                    }
                    case 5 -> {
                        if (worldX + sX <= eyeX != unobstructedPass) continue;
                        tes.setNormal(1, 0, 0);
                        tes.addVertex(X + sX, Y, Z);
                        tes.addVertex(X + sX, Y + sY, Z);
                        tes.addVertex(X + sX, Y + sY, Z + sZ);
                        tes.addVertex(X + sX, Y, Z + sZ);
                    }
                }
            }
        }
    }
}
