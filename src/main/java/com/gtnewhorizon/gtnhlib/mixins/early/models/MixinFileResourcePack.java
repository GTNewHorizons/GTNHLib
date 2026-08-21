package com.gtnewhorizon.gtnhlib.mixins.early.models;

import static com.gtnewhorizon.gtnhlib.core.GTNHLibCore.MODEL_LOGGER;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.PackMetadataSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

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

    /// Block old resourcepacks from loading blockstates or models.
    /// @return True if the resource SHOULDN'T load, false if it SHOULD.
    @Definition(id = "zipentry", local = @Local(type = ZipEntry.class))
    @Expression("zipentry == null")
    @ModifyExpressionValue(method = "getInputStreamByName", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private boolean nhlib$blockOldModels(boolean original, @Local(argsOnly = true) String resource) {
        // Return the default for the metadata, or getting it below will infinitely recurse.
        if (original || METADATA_FILE.equals(resource)) return original;

        if (!resource.endsWith(".json")) return false; // not a JSON blockstate
        var pathParts = resource.split("/");
        if (pathParts.length < 4) return false; // too short to be a blockstate
        if (!"assets".equals(pathParts[0])) return false;
        if (!"blockstates".equals(pathParts[2])) return false;

        // This song and dance is needed because some calls to getInputStreamByName expect it to never fail,
        // since they're referring to resources bundled in jars. So while the original has a checked exception, we can't
        // rely on everyone checking it.
        final int packFormat;
        IMetadataSection packFormatSection = null;
        try {
            packFormatSection = getPackMetadata(
                    Minecraft.getMinecraft().getResourcePackRepository().rprMetadataSerializer,
                    "pack");
        } catch (IOException e) {
            // If the above fails, pack metadata is null and thus format is assumed to be 1 (i.e. old).
        }
        if (!(packFormatSection instanceof PackMetadataSection metadata)) packFormat = 1;
        else packFormat = metadata.getPackFormat();

        // This is a blockstate, reject it if it's too old.
        return packFormat < PACK_FORMAT_MC_13_X;
    }
}
