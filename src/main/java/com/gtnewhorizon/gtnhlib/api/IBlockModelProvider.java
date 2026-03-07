package com.gtnewhorizon.gtnhlib.api;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;

public interface IBlockModelProvider {

    BakedModel getModel(BakedModelQuadContext context);
}
