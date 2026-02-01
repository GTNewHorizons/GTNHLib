package com.gtnewhorizon.gtnhlib.client.renderer;

import java.nio.ByteBuffer;

import net.minecraft.client.renderer.Tessellator;

public final class CallbackTessellator extends DirectTessellator {

    private DirectDrawCallback drawCallback;

    public CallbackTessellator(DirectDrawCallback callback) {
        super(Tessellator.byteBuffer);
        this.drawCallback = callback;
    }

    public CallbackTessellator(ByteBuffer initial, DirectDrawCallback callback) {
        super(initial);
        this.drawCallback = callback;
    }

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
        final int result = super.draw();
        if (drawCallback.onDraw(this)) {
            this.reset();
        }
        return result;
    }

    public void setDrawCallback(DirectDrawCallback drawCallback) {
        this.drawCallback = drawCallback;
    }
}
