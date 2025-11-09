package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;

public interface UnbakedModel {

    default BakedModel bake() {
        return bake(BakeData.IDENTITY);
    }

    BakedModel bake(BakeData data);
}
