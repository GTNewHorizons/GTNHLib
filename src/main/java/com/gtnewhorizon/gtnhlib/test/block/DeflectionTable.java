package com.gtnewhorizon.gtnhlib.test.block;

import static com.gtnewhorizon.gtnhlib.client.opengl.GLCaps.FBO;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

// How rays starting at one distance from the hole bend, one texel per starting angle. See table.frag.
final class DeflectionTable {

    static final int WIDTH = 4096;

    private int texture;
    private int framebuffer;
    private float start = Float.NaN;

    int texture() {
        return texture;
    }

    void invalidate() {
        start = Float.NaN;
    }

    // Needs the table shader bound. Leaves the viewport at the table's size.
    void fill(float start, int uStart, BlackHoleMesh mesh) {
        if (start == this.start) return;
        if (texture == 0) create();
        final int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(0, 0, WIDTH, 1);
        GL20.glUniform1f(uStart, start);
        mesh.drawScreen();
        FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
        this.start = start;
    }

    private void create() {
        final int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL30.GL_RGBA32F,
                WIDTH,
                1,
                0,
                GL11.GL_RGBA,
                GL11.GL_FLOAT,
                (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);

        framebuffer = FBO.glGenFramebuffers();
        final int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        FBO.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
    }
}
