package com.gtnewhorizon.gtnhlib.color;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.GTNHLib;

/**
 * A color constant supporting both ARGB and RGB formats, with resource pack override and per-instance caching.
 * <p>
 * Use {@link Factory} to avoid repeating the mod ID on every line:
 * 
 * <pre>
 * <code>
 *  public static class MyColors {
 *      private static final ColorResource.Factory color = new ColorResource.Factory("mymod");
 *
 *      public static final ColorResource
 *      // spotless:off
 *          background      = color.rgb("background",       "202020"),
 * 
 *          guiOverlayWhite = color.argb("guiOverlayWhite", "80FFFFFF")
 *          text            = color.argb("text",            "FFFFFFFF");
 *      // spotless:on
 *  }
 * </code>
 * </pre>
 * <p>
 * Then use the color anywhere:
 * 
 * <pre>
 * <code>
 *  GuiDraw.drawRect(x, y, w, h, MyColors.background.getColor());
 * </code>
 * </pre>
 * <p>
 * Resource packs override colors via a lang file entry: {@code color.resource.mymod.background=80FF20AA}
 * <p>
 * ARGB colors use 8-char hex (AARRGGBB). RGB colors use 6-char hex (RRGGBB), alpha is always FF.
 * <p>
 * The color cache is cleared automatically on F3+T via {@link CacheReloadListener}, registered by GTNHLib's client
 * proxy.
 */
public class ColorResource {

    private static final Set<ColorResource> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());

    private final String langKey;
    private final int defaultColor;
    private final boolean argb;
    private volatile long cachedColor = -1L;

    /**
     * @param modId the mod ID used as the namespace in the lang key
     * @param name  the color name used in the lang key
     * @param hex   default color — AARRGGBB if {@code argb} is true, RRGGBB otherwise
     * @param argb  true to include the alpha channel, false to force alpha to FF
     */
    public ColorResource(String modId, String name, String hex, boolean argb) {
        this.langKey = "color.resource." + modId + "." + name;
        this.argb = argb;
        this.defaultColor = parseHex(hex, argb);
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    private static int parseHex(String hex, boolean argb) {
        try {
            long value = Long.parseLong(hex.trim(), 16);
            return argb ? (int) value : (int) (0xFF000000L | value);
        } catch (NumberFormatException e) {
            GTNHLib.LOG.warn("[ColorResource] Invalid hex '{}', using opaque white.", hex);
            return 0xFFFFFFFF;
        }
    }

    /** Lang key used to look up a resource pack override. */
    public String getLangKey() {
        return langKey;
    }

    /**
     * Returns the resolved ARGB color value. Checks the resource pack lang file first; falls back to the default color.
     * Result is cached until the next resource reload (F3+T).
     * <p>
     * Example usage:
     * 
     * <pre>
     * <code>
     *  GuiDraw.drawRect(x, y, w, h, MyColors.background.getColor());
     * </code>
     * </pre>
     */

    public int getColor() {
        if (cachedColor != -1L) return (int) cachedColor;

        int color;
        if (StatCollector.canTranslate(langKey)) {
            String value = StatCollector.translateToLocal(langKey).trim();
            try {
                long parsed = Long.parseLong(value, 16);
                color = argb ? (int) parsed : (int) (0xFF000000L | parsed);
            } catch (NumberFormatException e) {
                GTNHLib.LOG.warn("[ColorResource] Invalid hex '{}' for lang key '{}', using default.", value, langKey);
                color = defaultColor;
            }
        } else {
            color = defaultColor;
        }

        cachedColor = color;
        return color;
    }

    /**
     * Factory that holds a mod ID so it does not need to be repeated on every color declaration.
     * <p>
     * Example usage:
     * 
     * <pre>
     * <code>
     *  private static final ColorResource.Factory colors = new ColorResource.Factory("mymod");
     *  public static final ColorResource
     *      background = colors.argb("background", "FF202020"),
     *      text       = colors.rgb("text",        "FFFFFF");
     * </code>
     * </pre>
     */
    public static class Factory {

        private final String modId;

        public Factory(String modId) {
            this.modId = modId;
        }

        /** Creates an ARGB color (AARRGGBB hex). */
        public ColorResource argb(String name, String hex) {
            return new ColorResource(modId, name, hex, true);
        }

        /** Creates an RGB color (RRGGBB hex), alpha is always FF. */
        public ColorResource rgb(String name, String hex) {
            return new ColorResource(modId, name, hex, false);
        }
    }

    /** Clears the color cache on resource reload (F3+T). Registered by GTNHLib's client proxy. */
    public static class CacheReloadListener implements IResourceManagerReloadListener {

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
            synchronized (INSTANCES) {
                for (ColorResource instance : INSTANCES) {
                    instance.cachedColor = -1L;
                }
            }
        }
    }
}
