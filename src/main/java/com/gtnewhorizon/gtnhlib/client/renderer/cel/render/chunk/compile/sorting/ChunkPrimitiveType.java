package com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.compile.sorting;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.sorting.TranslucentQuadAnalyzer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.ByteBuffer;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public interface ChunkPrimitiveType {
    default int getIndexBufferSize(int numPrimitives) {
        return numPrimitives * getIndexBufferElementsPerPrimitive() * 4;
    }

    /**
     * {@return the number of vertices in a given primitive, e.g. 4 for quads, 3 for triangles}
     */
    int getVerticesPerPrimitive();

    /**
     * {@return the number of index buffer elements per given primitive, e.g. 6 for quads, 3 for triangles}
     */
    int getIndexBufferElementsPerPrimitive();

    /**
     * Generate a "simple" index buffer for rendering numPrimitives primitives in the order they appear in the vertex
     * buffer.
     *
     * The caller is responsible for providing a buffer of size given by {@link ChunkPrimitiveType#getIndexBufferSize(int)}.
     * @param indexBuffer a NativeBuffer that should be populated with 32-bit integers
     * @param numPrimitives the number of primitives to generate the buffer for
     */
    void generateSimpleIndexBuffer(ByteBuffer indexBuffer, int numPrimitives);

    /**
     * Generate a sorted index buffer for numPrimitives primitives, with data on the primitives provided in chunkData.
     * <p>
     * The caller is responsible for providing a buffer of size given by {@link ChunkPrimitiveType#getIndexBufferSize(int)}.
     * <p>
     * The provided camera position will be subchunk-relative, and thus makes sense to compare directly with the vertex
     * positions provided in the SortState.
     * @param indexBuffer a NativeBuffer that should be populated with 32-bit integers
     * @param numPrimitives the number of primitives to generate the buffer for
     * @param chunkData the sorting data for the given primitives
     * @param x x position of the camera
     * @param y y position of the camera
     * @param z z position of the camera
     */
    void generateSortedIndexBuffer(ByteBuffer indexBuffer, int numPrimitives, @Nullable TranslucentQuadAnalyzer.SortState chunkData, float x, float y, float z);

    default List<String> getDefines() {
        return ObjectArrayList.of();
    }
}
