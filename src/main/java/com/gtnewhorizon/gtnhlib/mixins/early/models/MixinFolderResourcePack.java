package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry.MODEL_LOGGER;
import static java.nio.file.Files.walk;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelResourcePack;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.file.FileVisitOption;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import org.spongepowered.asm.mixin.Mixin;

@SuppressWarnings("UnusedMixin")
@Mixin(FolderResourcePack.class)
public abstract class MixinFolderResourcePack extends AbstractResourcePack implements ModelResourcePack {

    public MixinFolderResourcePack(File folder) {
        super(folder);
    }

    @Override
    public List<String> nhlib$getReferencedTextures(Function<Reader, JSONModel> jsonParser) {
        try (var files = walk(resourcePackFile.toPath(), FileVisitOption.FOLLOW_LINKS)) {

            final var jsons = files.filter(p -> {
                // Get the relative path
                final var path = resourcePackFile.toPath().relativize(p);

                // Make sure it's long enough (<domain>/<subdomain>/something.json), make sure the subdomain is
                // "models", make sure it's a file, and make sure it's a JSON
                if (path.getNameCount() < 3) return false;
                if (!path.getName(1).toString().equals("models")) return false;
                if (!path.toFile().isFile()) return false;
                return path.endsWith(".json");
            }).map(p -> {
                try {
                    return new FileInputStream(p.toFile());
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

            return nhlib$getReferencedTextures(jsons, jsonParser);
        } catch (Exception e) {
            MODEL_LOGGER.warn("Failed to walk resource pack {}", this);
            MODEL_LOGGER.warn(e);
        }

        return ModelResourcePack.super.nhlib$getReferencedTextures(jsonParser);
    }
}
