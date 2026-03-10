package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry.MODEL_LOGGER;

import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelResourcePack;
import com.gtnewhorizon.gtnhlib.client.model.loading.RPInfo;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;

@SuppressWarnings("UnusedMixin")
@Mixin(FileResourcePack.class)
public abstract class MixinFileResourcePack extends AbstractResourcePack implements ModelResourcePack {

    @Shadow
    protected abstract ZipFile getResourcePackZipFile() throws IOException;

    public MixinFileResourcePack(File file) {
        super(file);
    }

    @Override
    public RPInfo nhlib$gatherModelInfo(Function<Reader, JSONModel> jsonParser) {
        List<String> textures;
        final var models = new ArrayList<String>();

        try {
            var zip = getResourcePackZipFile();
            var jsons = zip.stream().filter(ze -> {
                final var name = ze.getName();
                final var parts = name.split("/");

                // If it's a blockstate file, record the block and move on
                if (parts.length < 4) return false;
                if (ze.isDirectory()) return false;
                if (!name.endsWith(".json")) return false;
                if (parts[2].equals("blockstates")) { // file is assets/<domain>/blockstates/someblock.json
                    models.add(parts[3].split("\\.")[0]);
                    return false;
                } else return parts[2].equals("models"); // file is assets/<domain>/models/someblock.json
            }).map(ze -> {
                try {
                    return zip.getInputStream(ze);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            textures = nhlib$getReferencedTextures(jsons, jsonParser);
        } catch (Exception e) {

            MODEL_LOGGER.warn("Failed to walk resource pack {}", this);
            MODEL_LOGGER.warn(e);
            textures = ObjectLists.emptyList();
        }

        return new RPInfo(textures, models);
    }
}
