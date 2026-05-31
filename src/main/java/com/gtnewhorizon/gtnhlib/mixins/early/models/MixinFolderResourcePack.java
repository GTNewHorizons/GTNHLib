package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static com.gtnewhorizon.gtnhlib.core.GTNHLibCore.MODEL_LOGGER;
import static java.nio.file.Files.walk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.file.FileVisitOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FolderResourcePack;

import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelResourcePack;
import com.gtnewhorizon.gtnhlib.client.model.loading.RPInfo;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;

import it.unimi.dsi.fastutil.objects.ObjectLists;

@SuppressWarnings("UnusedMixin")
@Mixin(FolderResourcePack.class)
public abstract class MixinFolderResourcePack extends AbstractResourcePack implements ModelResourcePack {

    public MixinFolderResourcePack(File folder) {
        super(folder);
    }

    @Override
    public RPInfo nhlib$gatherModelInfo(Function<Reader, JSONModel> jsonParser) {
        List<String> textures;
        final var models = new ArrayList<String>();

        try (var files = walk(resourcePackFile.toPath(), FileVisitOption.FOLLOW_LINKS)) {
            // This does two things while walking the files:
            // 1) Enumerate blockstate files and store their names
            // 2) Collect InputStreams from the model files, and pass those on for further analysis
            final var jsons = files.filter(path -> {
                // Get the relative path
                final var relPath = resourcePackFile.toPath().relativize(path);

                // Make sure it's long enough (assets/<namespace>/models/something.json), make sure the subnamespace is
                // "models", make sure it's a file, and make sure it's a JSON
                if (relPath.getNameCount() < 4) return false;
                if (!path.toFile().isFile()) return false;

                final var filename = path.getFileName().toString();
                if (!filename.endsWith(".json")) return false;

                final var subdomain = relPath.getName(2).toString();
                if (subdomain.equals("blockstates")) {
                    models.add(relPath.getName(1) + ":" + filename.split("\\.")[0]);
                    return false;
                } else return subdomain.equals("models");
            }).map(p -> {
                try {
                    return new FileInputStream(p.toFile());
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

            // This is the "further analysis". We read the files and scrape them for textures to load.
            textures = nhlib$getReferencedTextures(jsons, jsonParser);
        } catch (Exception e) {
            MODEL_LOGGER.warn("Failed to walk resource pack {}", this);
            MODEL_LOGGER.warn(e);
            textures = ObjectLists.emptyList();
        }

        return new RPInfo(textures, models);
    }
}
