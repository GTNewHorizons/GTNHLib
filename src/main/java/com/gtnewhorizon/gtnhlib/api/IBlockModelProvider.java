package com.gtnewhorizon.gtnhlib.api;

import net.minecraft.block.Block;

import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;

/**
 * An interface for {@link Block}s to allow providing custom models for {@link ModelISBRH}. Not necessary for standard
 * JSON models. This effectively allows for overwriting {@link ModelRegistry#getBakedModel} without needing a child
 * class, which avoids needing a new render ID (and associated mixins).
 */
public interface IBlockModelProvider {

    BakedModel getModel(BakedModelQuadContext context);
}
