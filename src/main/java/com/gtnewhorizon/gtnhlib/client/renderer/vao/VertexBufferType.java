package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL44;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IEmptyVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public enum VertexBufferType implements VertexBufferFactory {
    /**
     * Allows the contents to be mutated via {@link IVertexBuffer#update} & allows the buffer to grow/shrink via
     * {@link IEmptyVertexBuffer#allocate} or {@link com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer#update}
     */
    MUTABLE_RESIZABLE {

        @Override
        public final IVertexBuffer allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount) {
            return VAOManager.allocateMutableVAO(format, drawMode, data, vertexCount);
        }
    },
    /**
     * Allows the contents to be mutated via {@link IVertexBuffer#update}, but does <strong>NOT</strong> allow
     * {@link IEmptyVertexBuffer#allocate} calls. <br>
     * May improve performance.
     */
    MUTABLE {

        @Override
        public final IVertexBuffer allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount) {
            return VAOManager.allocateStorageVAO(format, drawMode, data, vertexCount, GL44.GL_DYNAMIC_STORAGE_BIT);
        }
    },
    /**
     * Does <strong>NOT</strong> allow mutations of the contents via {@link IVertexBuffer#update} and does
     * <strong>NOT</strong> allow {@link IEmptyVertexBuffer#allocate} calls. <br>
     * May improve performance.
     */
    IMMUTABLE {

        @Override
        public final IVertexBuffer allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount) {
            return VAOManager.allocateStorageVAO(format, drawMode, data, vertexCount, 0);
        }
    };
}
