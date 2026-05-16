package com.gtnewhorizon.gtnhlib.client.renderer;

import static com.gtnewhorizon.gtnhlib.client.renderer.tessellator.VertexCallbackManager.callback;

import java.nio.ByteBuffer;

import com.gtnewhorizon.gtnhlib.client.renderer.tessellator.VertexCallbackManager;

public final class CallbackTessellator extends DirectTessellator {

    public CallbackTessellator(ByteBuffer initial) {
        super(initial);
    }

    public CallbackTessellator(int capacity) {
        super(capacity);
    }

    public CallbackTessellator(ByteBuffer initial, boolean deleteAfter) {
        super(initial, deleteAfter);
    }

    @Override
    public int draw() {
        if (callback.onDraw(this)) {
            final int result = super.draw();
            reset();
            return result;
        }

        return 0; // onDraw returns false -> no draw happening
    }

    @Override
    public void startDrawing(int drawMode) {
        if (callback.onStartDrawing(this, drawMode)) {
            reset();
            this.isDrawing = true;
            this.drawMode = drawMode;
        }
    }

    @Override
    public void addVertex(double x, double y, double z) {
        if (format == null) {
            this.format = getOptimalVertexFormat();
        }

        ensureCapacity(this.format.getVertexSize());

        callback.onVertex(this, x, y, z);
        this.vertexCount++;
    }

    @Override
    public void setTextureUV(double u, double v) {
        if (!hasTexture) {
            if (preDefinedFormat != null) return;

            this.hasTexture = true;

            if (format != null) {
                fixBufferFormat();
            }
        }

        callback.onTextureUV(this, u, v);
    }

    @Override
    public void setNormal(float nx, float ny, float nz) {
        if (!hasNormals) {
            if (preDefinedFormat != null) return;

            this.hasNormals = true;

            if (format != null) {
                fixBufferFormat();
            }
        }

        callback.onNormal(this, nx, ny, nz);
    }

    @Override
    public void setColorRGBA(int red, int green, int blue, int alpha) {
        if (this.isColorDisabled) return;

        if (!this.hasColor) {
            if (preDefinedFormat != null) return;

            this.hasColor = true;

            if (format != null) {
                fixBufferFormat();
            }
        }

        if (red > 255) {
            red = 255;
        } else if (red < 0) {
            red = 0;
        }

        if (green > 255) {
            green = 255;
        } else if (green < 0) {
            green = 0;
        }

        if (blue > 255) {
            blue = 255;
        } else if (blue < 0) {
            blue = 0;
        }

        if (alpha > 255) {
            alpha = 255;
        } else if (alpha < 0) {
            alpha = 0;
        }

        callback.onColor(this, red, green, blue, alpha);
    }

    @Override
    protected void onRemovedFromStack() {
        super.onRemovedFromStack();
        VertexCallbackManager.popCallback();
    }

    public double getXOffset() {
        return this.xOffset;
    }

    public double getYOffset() {
        return this.yOffset;
    }

    public double getZOffset() {
        return this.zOffset;
    }
}
