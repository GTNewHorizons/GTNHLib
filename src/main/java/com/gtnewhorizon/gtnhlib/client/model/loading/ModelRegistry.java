package com.gtnewhorizon.gtnhlib.client.model.loading;

import static com.gtnewhorizon.gtnhlib.GTNHLibConfig.modelCacheSize;
import static com.gtnewhorizon.gtnhlib.client.model.unbaked.MissingModel.MISSING_MODEL;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.client.event.TextureStitchEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtnewhorizon.gtnhlib.blockstate.core.BlockState;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.state.MissingState;
import com.gtnewhorizon.gtnhlib.client.model.state.StateDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.state.StateModelMap;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.concurrent.ThreadsafeCache;

import cpw.mods.fml.common.FMLContainerHolder;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

/// Handles model loading and caching. All caches are size-based - this means that if a model has enough parents, it may
/// exhaust the caches and unload itself before being fully baked. There *probably* won't be any consequences for this
/// beyond excessively complex models being loaded multiple times... add a counter if I'm wrong.
public class ModelRegistry {

    public static final Logger MODEL_LOGGER = LogManager.getLogger(ModelRegistry.class);
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(StateModelMap.class, new StateDeserializer())
            .registerTypeAdapter(JSONModel.class, new ModelDeserializer()).create();

    /// The first cache. Ideally, every request hits this and gets a baked model.
    private static final ThreadsafeCache<BlockState, BakedModel> BLOCKSTATE_MODEL_CACHE = new ThreadsafeCache<>(
            modelCacheSize,
            s -> bakeModel((BlockState) s),
            false);

    /// If the first cache misses, we hit this to get the state map, so we can figure out which model to bake.
    private static final ThreadsafeCache<ResourceLoc.StateLoc, StateModelMap> STATE_MODEL_MAP_CACHE = new ThreadsafeCache<>(
            modelCacheSize,
            s -> ((ResourceLoc.StateLoc) s).load(() -> MissingState.MISSING_STATE_MAP, GSON),
            false);

    /// {@link JSONModel}s may be shared across several {@link BakedModel}s, so we cache them too. The cache ensures all
    /// loaded models are resolved and ready to bake.
    ///
    /// Note: the retriever was broken out into a function, because you can't read a field from within its own
    /// definition.
    private static final ThreadsafeCache<ResourceLoc.ModelLoc, JSONModel> JSON_MODEL_CACHE = new ThreadsafeCache<>(
            modelCacheSize,
            s -> loadAndResolveJSONModel((ResourceLoc.ModelLoc) s),
            false);

    private static BakedModel bakeModel(BlockState state) {
        final Block block = state.getBlock();
        final StateModelMap smm = getStateModelMap(block);

        // Caching this would be a little pointless, since an UnbakedModel here would map directly to the BakedModel
        // missing from the cache... that's why we're loading one from scratch. The JSONModel *used* by the UnbakedModel
        // will be cached, however.
        final var dough = smm.selectModel(state);
        if (dough == null) return MISSING_MODEL.bake();

        return dough.bake();
    }

    /// Getter for {@link BakedModel}s. We don't want to publicly expose the cache, modders can't be trusted with it :P
    public static BakedModel getBakedModel(BlockState state) {
        return BLOCKSTATE_MODEL_CACHE.get(state.clone());
    }

    /// Getter for {@link JSONModel}s. See {@link ModelRegistry#getBakedModel(BlockState)}
    public static JSONModel getJSONModel(ResourceLoc.ModelLoc loc) {
        return JSON_MODEL_CACHE.get(loc);
    }

    /// Registers the given mod ID for automatic texture loading. The resource pack attached to this mod will be scanned
    /// for model files, and textures from those files will be automatically loaded.
    public static void registerModid(String modid) {
        ReloadListener.PERMITTED_MODIDS.add(modid);
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

    private static final class BlockName {

        private final String domain;
        private final String name;

        private BlockName(String domain, String name) {
            this.domain = domain;
            this.name = name;
        }

        private static BlockName fromBlock(Block block) {
            final String blockName = Block.blockRegistry.getNameForObject(block);

            final int sepIdx = blockName.indexOf(":");
            final var domain = blockName.substring(0, sepIdx);
            final var name = blockName.substring(sepIdx + 1);

            return new BlockName(domain, name);
        }
    }

    public static class ReloadListener implements IResourceManagerReloadListener {

        private static final ObjectOpenHashSet<String> PERMITTED_MODIDS = new ObjectOpenHashSet<>();

        @Override
        public void onResourceManagerReload(IResourceManager irm) {
            // Flush all caches, we may have new models
            BLOCKSTATE_MODEL_CACHE.clear();
            STATE_MODEL_MAP_CACHE.clear();
            JSON_MODEL_CACHE.clear();

            if (!(irm instanceof GlobalResourceManager manager)) {
                return;
            }

            loadModelInfo(manager);
        }

        private boolean infoMixinFailed = false;

        private void loadModelInfo(GlobalResourceManager manager) {
            // Scan resource packs for model files
            final var domains = manager.nhlib$getDomainResourceManagers();
            final var resourcePacks = new ObjectOpenHashSet<IResourcePack>();
            for (var entry : domains.entrySet()) {
                var files = entry.getValue();
                if (files instanceof FallbackResourceManager) {
                    resourcePacks.addAll(((BackingResourceManager) files).nhlib$getResourcePacks());
                }
            }

            // Gather the list of modeled blocks and their textures
            final var modeledBlocks = new ObjectOpenHashSet<String>();
            final var texturesToLoad = new ObjectArrayList<String>();
            for (var pack : resourcePacks) {
                if (!(pack instanceof ModelResourcePack mrp)) continue;

                // Skip unregistered mods
                if (mrp instanceof FMLContainerHolder fmlch
                        && !PERMITTED_MODIDS.contains(fmlch.getFMLContainer().getModId()))
                    continue;

                final var info = mrp.nhlib$gatherModelInfo(reader -> GSON.fromJson(reader, JSONModel.class));
                modeledBlocks.addAll(info.modeledBlocks());
                texturesToLoad.addAll(info.textureNames());
            }

            GameData.getBlockRegistry().registryObjects.forEach((s, b) -> {
                if (!(s instanceof String name)) return;
                if (!(b instanceof BlockModelInfo block)) {
                    if (!infoMixinFailed) {
                        MODEL_LOGGER.error("Block registry contained a non-block or the info mixin failed!");
                        MODEL_LOGGER.error("Either you won't notice anything, or all JSON models will stop loading...");
                        infoMixinFailed = true;
                    }
                    return;
                }
                block.nhlib$setModeled(modeledBlocks.contains(modeledBlocks.contains(name)));
            });

            EventHandler.texturesToLoad = texturesToLoad;
        }
    }

    public static class EventHandler {

        private static @NotNull List<String> texturesToLoad = ObjectLists.emptyList();

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void onTextureStitch(TextureStitchEvent.Pre event) {
            for (var texture : texturesToLoad) {
                event.map.registerIcon(texture.replaceFirst("^minecraft:", ""));
            }
            texturesToLoad = ObjectLists.emptyList(); // don't need it anymore
        }
    }
}
