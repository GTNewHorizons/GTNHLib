package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry.MODEL_LOGGER;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizon.gtnhlib.client.model.loading.ModelResourcePack;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;

import it.unimi.dsi.fastutil.objects.ObjectLists;

@SuppressWarnings("UnusedMixin")
@Mixin(FileResourcePack.class)
public abstract class MixinFileResourcePack extends AbstractResourcePack implements ModelResourcePack {

    @Shadow
    protected abstract ZipFile getResourcePackZipFile() throws IOException;

    public MixinFileResourcePack(File file) {
        super(file);
    }

    @Override
    public List<String> nhlib$getReferencedTextures(Function<Reader, JSONModel> jsonParser) {
        try {
            var zip = getResourcePackZipFile();
            final var jsons = zip.stream().filter(ze -> {
                final var name = ze.getName();
                final var parts = name.split("/");

                // Make sure it's long enough (assets/<domain>/<subdomain>/something.json), make sure the subdomain is
                // "models", make sure it's a file, and make sure it's a JSON
                if (parts.length < 4) return false;
                if (!parts[2].equals("models")) return false;
                if (ze.isDirectory()) return false;
                return name.endsWith(".json");
            }).map(ze -> {

                try {
                    return zip.getInputStream(ze);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return nhlib$getReferencedTextures(jsons, jsonParser);
        } catch (Exception e) {

            MODEL_LOGGER.warn("Failed to walk resource pack {}", this);
            MODEL_LOGGER.warn(e);
        }

        return ObjectLists.emptyList();
    }
}
