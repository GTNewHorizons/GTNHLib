package com.gtnewhorizon.gtnhlib.client.renderer.postprocessing;

import org.lwjgl.opengl.GL11;

public class SharedDepthFramebuffer extends CustomFramebuffer {

    public SharedDepthFramebuffer(int settings) {
        super(settings);
    }

    public SharedDepthFramebuffer(int width, int height) {
        super(width, height);
    }

    public SharedDepthFramebuffer(int width, int height, int settings) {
        super(width, height, settings);
    }

    @Override
    protected final int createBufferBits() {
        return GL11.GL_COLOR_BUFFER_BIT;
    }
}
