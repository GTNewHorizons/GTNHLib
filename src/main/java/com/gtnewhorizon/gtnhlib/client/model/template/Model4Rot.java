package com.gtnewhorizon.gtnhlib.client.model.template;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.client.model.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.model.Variant;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.BakedModel;

/**
 * Use this to create JSON model rotatable in 4 directions - NSWE
 */
public class Model4Rot {

    public final BakedModel[] models = new BakedModel[4];
    private final Variant[] modelIds;

    public Model4Rot(ResourceLocation modelLoc) {

        this.modelIds = new Variant[] { new Variant(modelLoc, 0, 0, 0, false), new Variant(modelLoc, 0, 180, 0, false),
                new Variant(modelLoc, 0, 90, 0, false), new Variant(modelLoc, 0, 270, 0, false) };

        ModelLoader.registerModels(() -> loadModels(this), this.modelIds);
    }

    public static void loadModels(Model4Rot model) {
        for (int i = 0; i < 4; ++i) model.models[i] = ModelLoader.getModel(model.modelIds[i]);
    }
}
