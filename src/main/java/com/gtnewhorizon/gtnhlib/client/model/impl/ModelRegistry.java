package com.gtnewhorizon.gtnhlib.client.model.impl;

import java.io.IOException;
import java.io.InputStreamReader;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtnewhorizon.gtnhlib.block.BlockState;
import com.gtnewhorizon.gtnhlib.block.DynamicModelCache;
import com.gtnewhorizon.gtnhlib.client.model.BakedModel;
import com.gtnewhorizon.gtnhlib.json.StateDeserializer;
import com.gtnewhorizon.gtnhlib.json.StateModelMap;

public class ModelRegistry {

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(StateModelMap.class, new StateDeserializer())
            .create();
    private static final DynamicModelCache<BlockState> CACHE = new DynamicModelCache<>(
            s -> bakeModel((BlockState) s),
            false);

    private static BakedModel bakeModel(BlockState state) {
        final var block = state.block();
        final var meta = state.meta();

        // Fetch the blockstate file
        final var name = BlockName.fromBlock(block);
        final var stateLocation = new ResourceLocation(name.domain, "blockstates/" + name.name);

        final var resourceManager = Minecraft.getMinecraft().getResourceManager();
        try {
            final var stateResource = resourceManager.getResource(stateLocation);
            final var stateFile = GSON
                    .fromJson(new InputStreamReader(stateResource.getInputStream()), StateModelMap.class);
            // final var model = stateFile.selectModel()

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record BlockName(String domain, String name) {

        private static BlockName fromBlock(Block block) {
            final String blockName = Block.blockRegistry.getNameForObject(block);

            final int sepIdx = blockName.indexOf(":");
            final var domain = blockName.substring(0, sepIdx);
            final var name = blockName.substring(sepIdx);

            return new BlockName(domain, name);
        }
    }
}
