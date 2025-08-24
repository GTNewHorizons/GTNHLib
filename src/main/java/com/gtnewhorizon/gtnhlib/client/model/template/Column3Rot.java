package com.gtnewhorizon.gtnhlib.client.model.template;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.client.model.JSONVariant;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.model.BakedModel;

/**
 * Use this to create a full cube column rotatable in 3 directions - vertical, x, and z
 */
public class Column3Rot {

    public final JSONVariant[] jsonVariants = new JSONVariant[3];
    public final BakedModel[] models = new BakedModel[3];

    public Column3Rot(ResourceLocation model) {

        this.jsonVariants[0] = new JSONVariant(model, 0, 0, 0, false);
        this.jsonVariants[1] = new JSONVariant(model, 90, 90, 0, false);
        this.jsonVariants[2] = new JSONVariant(model, 90, 0, 0, false);

        ModelLoader.registerModels(
                () -> { for (int i = 0; i < 3; ++i) this.models[i] = ModelLoader.getModel(this.jsonVariants[i]); },
                this.jsonVariants);
    }

    public BakedModel updown() {
        return this.models[0];
    }

    public BakedModel eastwest() {
        return this.models[1];
    }

    public BakedModel northsouth() {
        return this.models[2];
    }
}
