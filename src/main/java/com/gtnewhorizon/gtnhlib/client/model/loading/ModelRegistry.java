package com.gtnewhorizon.gtnhlib.client.model.loading;

import static com.gtnewhorizon.gtnhlib.client.model.unbaked.MissingModel.MISSING_MODEL;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtnewhorizon.gtnhlib.client.model.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.state.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.state.MissingState;
import com.gtnewhorizon.gtnhlib.client.model.state.StateDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.state.StateModelMap;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.concurrent.ThreadsafeCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.block.Block;

/// Handles model loading and caching. All caches are size-based - this means that if a model has enough parents, it may
/// exhaust the caches and unload itself before being fully baked. There *probably* won't be any consequences for this
/// beyond excessively complex models being loaded multiple times... add a counter if I'm wrong.
public class ModelRegistry {

    private static Gson GSON = new GsonBuilder().registerTypeAdapter(StateModelMap.class, new StateDeserializer())
            .registerTypeAdapter(JSONModel.class, new ModelDeserializer()).create();

    /// The first cache. Ideally, every request hits this and gets a baked model.
    private static final ThreadsafeCache<BlockState, BakedModel> BLOCKSTATE_MODEL_CACHE = new ThreadsafeCache<>(
            s -> bakeModel((BlockState) s),
            false);

    /// If the first cache misses, we hit this to get the state map, so we can figure out which model to bake.
    private static final ThreadsafeCache<ResourceLoc.StateLoc, StateModelMap> STATE_MODEL_MAP_CACHE = new ThreadsafeCache<>(
            s -> ((ResourceLoc.StateLoc) s).load(() -> MissingState.MISSING_STATE_MAP, GSON),
            false);

    /// {@link JSONModel}s may be shared across several {@link BakedModel}s, so we cache them too. The cache ensures all
    /// loaded models are resolved and ready to bake.
    ///
    /// Note: the retriever was broken out into a function, because you can't read a field from within its own
    /// definition.
    private static final ThreadsafeCache<ResourceLoc.ModelLoc, JSONModel> JSON_MODEL_CACHE = new ThreadsafeCache<>(
            s -> loadAndResolveJSONModel((ResourceLoc.ModelLoc) s),
            false);

    private static final String[] DEFAULT_STATE_KEYS = new String[] { "meta" };

    private static BakedModel bakeModel(BlockState state) {
        final var block = state.block();
        final var meta = state.meta();

        final var smm = getStateModelMap(block);
        final var properties = new Object2ObjectArrayMap<String, String>(
                DEFAULT_STATE_KEYS,
                new String[] { Integer.toString(meta) });

        // Caching this would be a little pointless, since an UnbakedModel here would map directly to the BakedModel
        // missing from the cache... that's why we're loading one from scratch. The JSONModel *used* by the UnbakedModel
        // will be cached, however.
        final var dough = smm.selectModel(properties);
        if (dough == null) return MISSING_MODEL.bake();

        return dough.bake();
    }

    /// Getter for {@link BakedModel}s. We don't want to publicly expose the cache, modders can't be trusted with it :P
    public static BakedModel getBakedModel(BlockState state) {
        return BLOCKSTATE_MODEL_CACHE.get(state);
    }

    /// Getter for {@link JSONModel}s. See {@link ModelRegistry#getBakedModel(BlockState)}
    public static JSONModel getJSONModel(ResourceLoc.ModelLoc loc) {
        return JSON_MODEL_CACHE.get(loc);
    }

    private static StateModelMap getStateModelMap(Block block) {
        final var name = BlockName.fromBlock(block);
        final var stateLocation = new ResourceLoc.StateLoc(name.domain, name.name);
        return STATE_MODEL_MAP_CACHE.get(stateLocation);
    }

    private static JSONModel loadAndResolveJSONModel(ResourceLoc.ModelLoc loc) {
        final var m = loc.load(() -> MISSING_MODEL, GSON);
        m.resolveParents(JSON_MODEL_CACHE::get);
        return m;
    }

    @Desugar
    private record BlockName(String domain, String name) {

        private static BlockName fromBlock(Block block) {
            final String blockName = Block.blockRegistry.getNameForObject(block);

            final int sepIdx = blockName.indexOf(":");
            final var domain = blockName.substring(0, sepIdx);
            final var name = blockName.substring(sepIdx + 1);

            return new BlockName(domain, name);
        }
    }
}
