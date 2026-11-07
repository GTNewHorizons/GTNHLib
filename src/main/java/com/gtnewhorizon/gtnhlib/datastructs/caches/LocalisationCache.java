package com.gtnewhorizon.gtnhlib.datastructs.caches;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.client.ResourceReloadListener;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/// A generic framework for caching frequently accessed localisation keys.
///
/// All fields that correspond to a localisation string should be annotated with the {@link LangKey} annotation and be
/// assigned a default string value. You can ensure setter access by marking the field private and either writing your
/// own getters or using a lombok getter annotation.
public abstract class LocalisationCache {

    @Target({ FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface LangKey {

        @NotNull
        String value();
    }

    private final @NotNull Map<String, String> defaults = Collections.emptyMap();

    protected LocalisationCache() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            ResourceReloadListener.registerEarlyCallback(this::onResourceManagerReload);
        }
    }

    @SideOnly(Side.CLIENT)
    public void onResourceManagerReload(IResourceManager resourceManager) {
        // update the field values
        boolean areDefaultsLoaded = defaults.isEmpty();
        for (final Field field : this.getClass().getDeclaredFields()) {
            // we only care about string fields with the lang key annotation
            if (field.getType() != String.class) continue;
            LangKey annotation = field.getAnnotation(LangKey.class);
            if (annotation == null) continue;
            String langKey = annotation.value();

            // compute the default key if it doesn't exist.
            if (!areDefaultsLoaded) {
                defaults.computeIfAbsent(langKey, (x) -> {
                    try {
                        return (String) FieldUtils.readField(field, this, true);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            // attempt to translate, else fallback to the default value, and then they key itself.
            String translated;
            if (!StatCollector.canTranslate(langKey)) {
                translated = this.defaults.getOrDefault(annotation.value(), langKey);
            } else {
                translated = StatCollector.translateToLocal(annotation.value());
            }

            // update the field value
            try {
                FieldUtils.writeField(field, this, translated, true);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
