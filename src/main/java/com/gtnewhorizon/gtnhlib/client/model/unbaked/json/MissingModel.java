package com.gtnewhorizon.gtnhlib.client.model.unbaked.json;

import static com.gtnewhorizon.gtnhlib.client.model.baked.PileOfQuads.BLANK;

import java.util.HashMap;

import com.gtnewhorizon.gtnhlib.client.model.BakeData;
import com.gtnewhorizon.gtnhlib.client.model.BakedModel;

public class MissingModel extends JSONModel {

    public static final MissingModel MISSING_MODEL = new MissingModel();

    public MissingModel() {
        super(null, false, null, new HashMap<>(), null);
    }

    @Override
    public BakedModel bake(BakeData data) {
        return BLANK;
    }
}
