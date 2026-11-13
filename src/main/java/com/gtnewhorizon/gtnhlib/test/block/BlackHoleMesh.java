package com.gtnewhorizon.gtnhlib.test.block;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import com.gtnewhorizon.gtnhlib.client.opengl.UniversalVAO;

// A -1 to 1 cube with its faces pointing inwards, so it still draws with the camera inside, then one triangle covering
// the viewport.
final class BlackHoleMesh {

    private static final int CUBE_VERTICES = 36;

    private int vao;

    void drawCube() {
        draw(0, CUBE_VERTICES);
    }

    void drawScreen() {
        draw(CUBE_VERTICES, 3);
    }

    private void draw(int first, int count) {
        if (vao == 0) create();
        UniversalVAO.bindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, first, count);
        UniversalVAO.bindVertexArray(0);
    }

    private void create() {
        final FloatBuffer vertices = BufferUtils.createFloatBuffer((CUBE_VERTICES + 3) * 3);
        final float[][] corners = new float[4][3];
        for (int axis = 0; axis < 3; axis++) {
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {
                    final int k = side > 0 ? 3 - i : i;
                    corners[i][axis] = side;
                    corners[i][(axis + 1) % 3] = k == 1 || k == 2 ? 1 : -1;
                    corners[i][(axis + 2) % 3] = k >= 2 ? 1 : -1;
                }
                for (int i : new int[] { 0, 1, 2, 0, 2, 3 }) vertices.put(corners[i]);
            }
        }
        vertices.put(new float[] { -1, -1, 0, 3, -1, 0, -1, 3, 0 });
        vertices.flip();

        vao = UniversalVAO.genVertexArrays();
        UniversalVAO.bindVertexArray(vao);
        final int buffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        UniversalVAO.bindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
}
