package com.gtnewhorizon.gtnhlib.client.model.loading;

import static com.gtnewhorizon.gtnhlib.core.GTNHLibCore.MODEL_LOGGER;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

/// This class exist to inject textures into one or more atlases, even if they were never intended to be there.
public class TexHelper {

    static IIcon registerTexture(TextureMap atlas, String texture) {
        var icon = atlas.registerIcon(texture);
        if (!(icon instanceof BaseCheckedTex bCT)) {
            // Something has gone terribly wrong, and the map isn't returning a normal sprite
            MODEL_LOGGER.warn("Failed to register {} in foreign atlas {}", texture, atlas);
            return icon;
        }

        bCT.nhlib$setHasBase(false);
        return icon;
    }

    /// Injected into [TextureAtlasSprite] so we can check if it has an implied base like `items` or `blocks`.`
    public interface BaseCheckedTex {

        void nhlib$setHasBase(boolean hasBase);

        boolean nhlib$hasBase();
    }
}
