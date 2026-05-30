package com.gtnewhorizon.gtnhlib.color;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.GTNHLib;

/**
 * Interface for color enums with resource pack override support.
 * <p>
 * Declare a public {@code String} field holding the AARRGGBB hex and store it in the constructor:
 * 
 * <pre>
 * <code>
 * public enum CustomColor implements EnumColor {
 *      background("FF202020"),
 *      textColor("FFFFFFFF")
 *
 *      // Add more colors here
 *      ; // leave trailing semicolon
 * 
 *      public final String hex;
 *          CustomColor(String hex) { this.hex = hex; }
 * }
 * </code>
 * </pre>
 * <p>
 * The namespace in the lang key defaults to the enum's simple class name. Use {@link Mod} on the class to set a custom
 * one.
 * <p>
 * Resource packs override colors via a lang file entry (AARRGGBB hex, no prefix):
 * {@code gui.color.CustomColor.background=80FF20AA}
 * <p>
 * The color cache is cleared automatically on F3+T via {@link CacheReloadListener}, registered by GTNHLib's client
 * proxy.
 */
public interface EnumColor {

    /** Optional: sets the namespace used in the lang key. Defaults to the enum's simple class name. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Mod {

        String value();
    }

    Map<String, Integer> COLOR_CACHE = new ConcurrentHashMap<>();

    String name();

    /** Returns the mod namespace from {@link Mod} if present, otherwise the enum's simple class name. */
    default String getModId() {
        Mod mod = getClass().getAnnotation(Mod.class);
        return mod != null ? mod.value() : getClass().getSimpleName();
    }

    /** Lang key used to look up a resource pack override. */
    default String getLangKey() {
        return "gui.color." + getModId() + "." + name();
    }

    /**
     * Returns the resolved ARGB color value. Checks the resource pack lang file first; falls back to the hex string
     * stored in the enum's field. Result is cached until the next resource reload (F3+T).
     * <p>
     * Example usage:
     * 
     * <pre>
     * <code>
     * GuiDraw.drawRect(x, y, w, h, CustomColor.background.getColor());
     * </code>
     * </pre>
     */
    default int getColor() {
        String key = getLangKey();
        Integer cached = COLOR_CACHE.get(key);
        if (cached != null) return cached;

        int color;
        if (StatCollector.canTranslate(key)) {
            String value = StatCollector.translateToLocal(key).trim();
            try {
                color = (int) Long.parseLong(value, 16);
            } catch (NumberFormatException e) {
                GTNHLib.LOG.warn("[EnumColor] Invalid hex color '{}' for lang key '{}', using default.", value, key);
                color = readDefault();
            }
        } else {
            color = readDefault();
        }

        COLOR_CACHE.put(key, color);
        return color;
    }

    /**
     * Returns the default ARGB color by reading the first non-static {@code String} field declared in the implementing
     * enum. Falls back to opaque white (0xFFFFFFFF) if none is found.
     */
    default int readDefault() {
        for (Field f : getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic() || f.getType() != String.class) continue;
            f.setAccessible(true);
            try {
                String s = (String) f.get(this);
                if (s != null) return (int) Long.parseLong(s.trim(), 16);
            } catch (IllegalAccessException e) {
                GTNHLib.LOG.warn(
                        "[EnumColor] Could not access field '{}' on {}.{}.",
                        f.getName(),
                        getClass().getSimpleName(),
                        name());
            } catch (NumberFormatException e) {
                GTNHLib.LOG.warn(
                        "[EnumColor] Invalid hex color in field '{}' on {}.{}, using default.",
                        f.getName(),
                        getClass().getSimpleName(),
                        name());
            }
        }
        GTNHLib.LOG.warn(
                "[EnumColor] No String field found on {}.{}, returning opaque white.",
                getClass().getSimpleName(),
                name());
        return 0xFFFFFFFF;
    }

    /** Clears the color cache on resource reload (F3+T). Registered by GTNHLib's client proxy. */
    class CacheReloadListener implements IResourceManagerReloadListener {

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
            COLOR_CACHE.clear();
        }
    }
}
