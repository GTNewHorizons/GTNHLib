package com.gtnewhorizon.gtnhlib.client.renderer.vbo;

/**
 * A class that stores multiple vertex formats & their corresponding buffers.
 */
public final class BigVBO {

    private final SubVBO[] vbos;

    BigVBO(SubVBO[] vbos) {
        this.vbos = vbos;
    }

    public static BigVBO emptyVBO() {
        return new BigVBO(new BigVBO.SubVBO[0]);
    }

    public void delete() {
        for (SubVBO vbo : vbos) {
            vbo.delete();
        }
    }

    public void render(int index) {
        vbos[index].render();
    }

    static final class SubVBO {

        private final VertexBuffer vbo;
        private final int drawMode;
        private final int start;
        private final int count;

        public SubVBO(VertexBuffer vbo, int drawMode, int start, int count) {
            this.vbo = vbo;
            this.drawMode = drawMode;
            this.start = start;
            this.count = count;
        }

        public void delete() {
            vbo.delete();
        }

        public void render() {
            vbo.render(drawMode, start, count);
        }
    }
}
