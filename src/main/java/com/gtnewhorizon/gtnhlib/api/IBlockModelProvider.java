package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;

/// An interface for {@link Block}s to allow providing custom models for {@link ModelISBRH}. Not necessary for standard
/// JSON models. This allows for custom model detection and baking logic.
public interface IBlockModelProvider {

    BakedModel getModel(BakedModelQuadContext context);
}
