package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static com.gtnewhorizon.gtnhlib.core.GTNHLibCore.MODEL_LOGGER;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelResourcePack;
import com.gtnewhorizon.gtnhlib.client.model.loading.RPInfo;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

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
                    models.add(parts[1] + ":" + parts[3].split("\\.")[0]);
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

    @Unique
    private static final int PACK_FORMAT_MC_13_X = 4;
    @Unique
    private static final String METADATA_FILE = "pack.mcmeta";

    @Unique
    private int nhlib$packFormat = -1;

    /// Block old resourcepacks from loading blockstates or models.
    /// @return True if the resource SHOULDN'T load, false if it SHOULD.
    @Definition(id = "zipentry", local = @Local(type = ZipEntry.class))
    @Expression("zipentry == null")
    @ModifyExpressionValue(method = "getInputStreamByName", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private boolean nhlib$blockOldModels(boolean original, @Local(argsOnly = true) String resource) {
        // Return the default for the metadata, or getting it below will infinitely recurse.
        if (original || METADATA_FILE.equals(resource)) return original;

        // Not a JSON model or blockstate
        if (!resource.endsWith(".json") || !resource.startsWith("assets/")) return false;

        int firstSlash = 6; // Index of slash after "assets"
        int secondSlash = resource.indexOf('/', firstSlash + 1);
        if (secondSlash == -1 || !resource.startsWith("blockstates/", secondSlash + 1)) return false;

        // This song and dance is needed because some calls to getInputStreamByName expect it to never fail,
        // since they're referring to resources bundled in jars. So while the original has a checked exception, we can't
        // rely on everyone checking it.
        if (this.nhlib$packFormat == -1) {
            try (InputStream is = getInputStreamByName(METADATA_FILE)) {
                if (is != null) {
                    JsonObject json = new JsonParser().parse(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    if (json.has("pack")) {
                        JsonObject packObj = json.getAsJsonObject("pack");
                        if (packObj.has("pack_format")) {
                            this.nhlib$packFormat = packObj.get("pack_format").getAsInt();
                        }
                    }
                }
            } catch (Exception e) {
                // If the above fails, pack metadata is null and thus format is assumed to be 1 (i.e. old).
            }

            if (this.nhlib$packFormat == -1) {
                this.nhlib$packFormat = 1;
            }
        }

        if (GTNHLibConfig.enableModelDebugLogs) {
            if (this.nhlib$packFormat < PACK_FORMAT_MC_13_X) {
                MODEL_LOGGER.warn(
                        "Rejecting valid model at {} because its pack.mcmeta format is too old (is {}, must be >={})",
                        resource,
                        this.nhlib$packFormat,
                        PACK_FORMAT_MC_13_X);
            }
        }

        // Reject it if it's too old.
        return this.nhlib$packFormat < PACK_FORMAT_MC_13_X;
    }
}
