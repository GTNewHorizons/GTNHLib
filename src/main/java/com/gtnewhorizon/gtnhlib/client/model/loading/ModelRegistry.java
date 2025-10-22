package com.gtnewhorizon.gtnhlib.client.model.loading;

import static com.gtnewhorizon.gtnhlib.client.model.unbaked.MissingModel.MISSING_MODEL;
import static it.unimi.dsi.fastutil.objects.Object2ObjectMaps.unmodifiable;

import net.minecraft.block.Block;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.state.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.ModelLocation;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc.StateLocation;
import com.gtnewhorizon.gtnhlib.client.model.state.MissingState;
import com.gtnewhorizon.gtnhlib.client.model.state.StateDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.state.StateModelMap;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.concurrent.ThreadsafeCache;
import it.unimi.dsi.fastutil.Pair;

/// Handles model loading and caching. All caches are size-based - this means that if a model has enough parents, it may
/// exhaust the caches and unload itself before being fully baked. There *probably* won't be any consequences for this
/// beyond excessively complex models being loaded multiple times... add a counter if I'm wrong.
public class ModelRegistry {

    public static final Logger MODEL_LOGGER = LogManager.getLogger(ModelRegistry.class);
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(StateModelMap.class, new StateDeserializer())
            .registerTypeAdapter(JSONModel.class, new ModelDeserializer()).create();

    /// The first cache. Ideally, every request hits this and gets a baked model.
    private static final ThreadsafeCache<Pair<StateModelMap, String>, BakedModel> BLOCKSTATE_MODEL_CACHE = new ThreadsafeCache<>(ModelRegistry::bakeModel, false);

    /// If the first cache misses, we hit this to get the state map, so we can figure out which model to bake.
    private static final ThreadsafeCache<StateLocation, StateModelMap> STATE_MODEL_MAP_CACHE = new ThreadsafeCache<>(
            s -> s.load(() -> MissingState.MISSING_STATE_MAP, GSON),
            false);

    /// {@link JSONModel}s may be shared across several {@link BakedModel}s, so we cache them too. The cache ensures all
    /// loaded models are resolved and ready to bake.
    ///
    /// Note: the retriever was broken out into a function, because you can't read a field from within its own
    /// definition.
    private static final ThreadsafeCache<ModelLocation, JSONModel> JSON_MODEL_CACHE = new ThreadsafeCache<>(ModelRegistry::loadAndResolveJSONModel, false);

    /// Getter for {@link BakedModel}s. We don't want to publicly expose the cache, modders can't be trusted with it :P
    public static BakedModel getBakedModel(BlockState state) {
        final StateModelMap modelMap = getStateModelMap(state);

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

    /// Getter for {@link JSONModel}s. See {@link ModelRegistry#getBakedModel(BlockState)}
    public static JSONModel getJSONModel(ModelLocation loc) {
        return JSON_MODEL_CACHE.get(loc);
    }

    private static JSONModel loadAndResolveJSONModel(ModelLocation loc) {
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
