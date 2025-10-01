package com.gtnewhorizon.gtnhlib.client.model;

public interface UnbakedModel {

    default BakedModel bake() {
        return bake(BakeData.IDENTITY);
    }

    BakedModel bake(BakeData data);
}
