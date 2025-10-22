package com.gtnewhorizon.gtnhlib.client.model.loading;

import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ModelResourcePack {
    /// Returns a list of the texture names referenced by models in this resource pack.
    default List<String> nhlib$getReferencedTextures(Function<Reader, JSONModel> jsonParser) {
        return ObjectLists.emptyList();
    }

    default List<String> nhlib$getReferencedTextures(Stream<? extends InputStream> files, Function<Reader, JSONModel> jsonParser) {
        return files.flatMap(is -> {
            final var model = jsonParser.apply(new InputStreamReader(is));
            return model.getTextures().values().stream().filter(v -> {
                // Ignore texture variables
                return !v.startsWith("#");
            });
        }).collect(Collectors.toList());
    }
}
