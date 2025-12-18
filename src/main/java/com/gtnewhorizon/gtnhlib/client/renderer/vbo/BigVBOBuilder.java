package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import org.lwjgl.BufferUtils;

import com.gtnewhorizon.gtnhlib.client.renderer.vao.VAOManager;
import com.gtnewhorizon.gtnhlib.client.renderer.vertex.VertexFormat;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class BigVBOBuilder {

    private final Object2ObjectOpenHashMap<VertexFormat, FormatData> formats = new Object2ObjectOpenHashMap<>();
    private int count;

    public BigVBOBuilder addDraw(VertexFormat format, int drawMode, ByteBuffer buffer) {
        final int vertexCount = format.getVertexCount(buffer);

        FormatData data = formats.get(format);
        if (data == null) {
            data = new FormatData();
            formats.put(format, data);
        }
        data.buffers.add(buffer);
        data.vertexCounts.add(vertexCount);
        data.drawModes.add(drawMode);
        data.drawIndex.add(count);
        count++;
        return this;
    }

    public BigVBO build() {
        final BigVBO.SubVBO[] vbos = new BigVBO.SubVBO[count];
        for (Map.Entry<VertexFormat, FormatData> entry : formats.entrySet()) {
            final FormatData data = entry.getValue();
            VertexBuffer vbo = data.compileToBuffer(entry.getKey());
            int start = 0;
            for (int i = 0; i < data.drawIndex.size(); i++) {
                final int index = data.drawIndex.get(i);
                final int vertexCount = data.vertexCounts.get(i);
                vbos[index] = new BigVBO.SubVBO(vbo, data.drawModes.get(i), start, vertexCount);
                start += vertexCount;
            }
        }
        return new BigVBO(vbos);
    }

    private static final class FormatData {

        private final List<ByteBuffer> buffers = new ArrayList<>();
        private final List<Integer> drawModes = new ArrayList<>();
        private final List<Integer> vertexCounts = new ArrayList<>();
        private final List<Integer> drawIndex = new ArrayList<>();

        public VertexBuffer compileToBuffer(VertexFormat format) {
            VertexBuffer vao = VAOManager.createVAO(format, -1);
            if (buffers.size() == 1) {
                vao.upload(buffers.get(0));
            } else {
                int needed = 0;
                for (ByteBuffer buffer : buffers) {
                    needed += buffer.remaining();
                }
                final ByteBuffer out = BufferUtils.createByteBuffer(needed);
                for (ByteBuffer buffer : buffers) {
                    buffer.put(out);
                }
                vao.upload(out);
            }
            return vao;
        }
    }
}
