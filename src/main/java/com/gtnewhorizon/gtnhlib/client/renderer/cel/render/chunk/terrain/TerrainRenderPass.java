package com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.terrain;

import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.polyfill.Maps;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.compile.sorting.ChunkPrimitiveType;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.terrain.material.Material;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.vertex.format.ChunkVertexType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;

/**
 * A terrain render pass corresponds to a draw call to render some subset of terrain geometry. Passes are generally used
 * for fixed configuration that will not change from quad to quad and allow for optimizations to be made within the
 * terrain shader code at compile time (e.g. omitting the fragment discard conditional entirely on the solid pass).
 * <p>
 * </p>
 * Geometry that shares the same terrain render pass may still be able to specify some more dynamic properties. See
 * {@link Material} for more information.
 */
@Accessors(fluent = true)
@EqualsAndHashCode
public class TerrainRenderPass {

    /**
     * The friendly name of this render pass.
     */
    @Getter
    @EqualsAndHashCode.Exclude
    private final String name;

    /**
     * A callback used to set up/clear GPU pipeline state.
     */
    private final PipelineState pipelineState;

    /**
     * Whether sections on this render pass should be rendered farthest-to-nearest, rather than nearest-to-farthest.
     */
    private final boolean useReverseOrder;
    /**
     * Whether fragment alpha testing should be enabled for this render pass.
     */
    private final boolean fragmentDiscard;
    /**
     * Whether this render pass wants to opt in to translucency sorting if enabled.
     */
    private final boolean useTranslucencySorting;
    /**
     * Whether this render pass has no lightmap texture.
     */
    private final boolean hasNoLightmap;

    private final @NotNull ChunkPrimitiveType primitiveType;
    private final @NotNull ChunkVertexType vertexType;

    private final Map<String, String> extraDefines;

    @Builder
    public TerrainRenderPass(String name, PipelineState pipelineState, boolean useReverseOrder, boolean fragmentDiscard,
            boolean useTranslucencySorting, boolean hasNoLightmap, @NotNull ChunkVertexType vertexType,
            @NotNull ChunkPrimitiveType primitiveType, @Singular Map<String, String> extraDefines) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name not specified for terrain pass");
        }
        Objects.requireNonNull(vertexType);
        Objects.requireNonNull(primitiveType);

        this.name = name;
        this.pipelineState = pipelineState;
        this.useReverseOrder = useReverseOrder;
        this.fragmentDiscard = fragmentDiscard;
        this.useTranslucencySorting = useTranslucencySorting;
        this.hasNoLightmap = hasNoLightmap;
        this.primitiveType = primitiveType;
        this.vertexType = vertexType;
        this.extraDefines = Maps.copyOf(extraDefines);
    }

    public boolean isReverseOrder() {
        return this.useReverseOrder;
    }

    public boolean isSorted() {
        return this.useTranslucencySorting;
    }

    public boolean hasNoLightmap() {
        return this.hasNoLightmap;
    }

    public void startDrawing() {
        this.pipelineState.setup();
    }

    public void endDrawing() {
        this.pipelineState.clear();
    }

    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }

    public ChunkPrimitiveType primitiveType() {
        return this.primitiveType;
    }

    public ChunkVertexType vertexType() {
        return this.vertexType;
    }

    public Map<String, String> extraDefines() {
        return this.extraDefines;
    }

    @Override
    public String toString() {
        return "TerrainRenderPass[name=" + this.name + "]";
    }

    public interface PipelineState {

        PipelineState DEFAULT = new PipelineState() {

            @Override
            public void setup() {

            }

            @Override
            public void clear() {

            }
        };

        void setup();

        void clear();
    }
}
