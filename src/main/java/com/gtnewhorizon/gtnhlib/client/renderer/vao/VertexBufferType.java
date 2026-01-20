package com.gtnewhorizon.gtnhlib.client.renderer.vao;

import static com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager.*;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL44;

import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IEmptyVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.IVertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vbo.VertexBuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

public enum VertexBufferType implements VertexBufferFactory {
    /**
     * Allows the contents to be mutated via {@link IVertexBuffer#update} & allows the buffer to grow/shrink via
     * {@link IEmptyVertexBuffer#allocate}
     */
    MUTABLE_RESIZABLE {

        @Override
        public IVertexBuffer allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount) {
            return vaoEnabled ? new VertexArrayBuffer(format, drawMode, data, vertexCount)
                    : new VertexBuffer(format, drawMode, data, vertexCount);
        }
    },
    /**
     * Allows the contents to be mutated via {@link IVertexBuffer#update}, but does <strong>NOT</strong> allow
     * {@link IEmptyVertexBuffer#allocate} calls. <br>
     * May improve performance.
     */
    MUTABLE {

        @Override
        public IVertexBuffer allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount) {
            return vaoEnabled
                    ? new VertexArrayBufferStorage(format, drawMode, data, vertexCount, GL44.GL_DYNAMIC_STORAGE_BIT)
                    : new VertexBufferStorage(format, drawMode, data, vertexCount, GL44.GL_DYNAMIC_STORAGE_BIT);

        }
    },
    /**
     * Does <strong>NOT</strong> allow mutations of the contents via {@link IVertexBuffer#update} and does
     * <strong>NOT</strong> allow {@link IEmptyVertexBuffer#allocate} calls. <br>
     * May improve performance.
     */
    IMMUTABLE {

        @Override
        public IVertexBuffer allocate(VertexFormat format, int drawMode, ByteBuffer data, int vertexCount) {
            return vaoEnabled ? new VertexArrayBufferStorage(format, drawMode, data, vertexCount, 0)
                    : new VertexBufferStorage(format, drawMode, data, vertexCount, 0);

        }
    };
}
