package com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad;

import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.render.chunk.sprite.SpriteTransparencyLevel;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();

    boolean hasShade();

    void addFlags(int flags);

    int getVerticesCount();

    @Nullable SpriteTransparencyLevel getTransparencyLevel();

    static BakedQuadView of(Object o) {
        return (BakedQuadView)o;
    }

    @SuppressWarnings("unchecked")
    static <T> List<? extends BakedQuadView> ofList(List<T> quads) {
        return (List<? extends BakedQuadView>)(List<?>)quads;
    }
}
