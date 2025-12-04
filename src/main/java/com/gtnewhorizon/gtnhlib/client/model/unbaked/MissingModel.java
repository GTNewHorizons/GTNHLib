package com.gtnewhorizon.gtnhlib.client.model.unbaked;

import static com.gtnewhorizon.gtnhlib.client.model.baked.PileOfQuads.BLANK;

import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;

public class MissingModel extends JSONModel {

    public static final MissingModel MISSING_MODEL = new MissingModel();

    public MissingModel() {
        super(null, false, null, Object2ObjectMaps.emptyMap(), null);
    }

    @Override
    public BakedModel bake(BakeData data) {
        return BLANK;
    }
}
