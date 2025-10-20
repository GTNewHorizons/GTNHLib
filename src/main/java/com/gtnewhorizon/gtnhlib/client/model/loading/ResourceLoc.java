package com.gtnewhorizon.gtnhlib.client.model.loading;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.client.model.state.StateModelMap;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.json.JSONModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface ResourceLoc<T> {

    default T load(Supplier<@NotNull T> defaultSrc, Gson gson) {
        final var jsonPath = new ResourceLocation(owner(), prefix() + path() + ext());

        try {
            final InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(jsonPath).getInputStream();
            return gson.fromJson(new InputStreamReader(is), clazz());
        } catch (IOException e) {

            GTNHLib.LOG.error("Could not find {}:{}", owner(), jsonPath.getResourcePath());
            return defaultSrc.get();
        }
    }

    String prefix();

    String owner();

    String path();

    String ext();

    Class<T> clazz();

    @Desugar
    record ModelLoc(String owner, String path) implements ResourceLoc<JSONModel> {

        public static ModelLoc fromStr(String id) {
            final int sepIdx = id.indexOf(':');
            if (sepIdx < 0) return new ModelLoc("minecraft", id);
            return new ModelLoc(id.substring(0, sepIdx), id.substring(sepIdx + 1));
        }

        @Override
        public String prefix() {
            return "models/";
        }

        @Override
        public String ext() {
            return ".json";
        }

        @Override
        public Class<JSONModel> clazz() {
            return JSONModel.class;
        }
    }

    @Desugar
    record StateLoc(String owner, String path) implements ResourceLoc<StateModelMap> {

        @Override
        public String prefix() {
            return "blockstates/";
        }

        @Override
        public String ext() {
            return ".json";
        }

        @Override
        public Class<StateModelMap> clazz() {
            return StateModelMap.class;
        }
    }
}
