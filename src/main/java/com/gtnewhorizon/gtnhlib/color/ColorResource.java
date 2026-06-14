package com.gtnewhorizon.gtnhlib.color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A color constant supporting both ARGB and RGB formats, with resource pack override and per-instance caching.
 * <p>
 * Use {@link Factory} to avoid repeating the mod ID on every line. The string passed to {@link Factory} must be the
 * mod's assets folder name - a plain lowercase string matching the folder under {@code assets/}, e.g.
 * {@code "appliedenergistics2"}, {@code "thaumicenergistics"}.
 *
 * <pre>
 * <code>
 *  public static class ColorUtils {
 *      private static final ColorResource.Factory color = new ColorResource.Factory("mymod"); // your assets folder name
 *
 *      public static final ColorResource
 *      // spotless:off
 *          background      = color.rgb("background",       "0x202020"),
 *
 *          guiOverlayWhite = color.argb("guiOverlayWhite", "0x80FFFFFF"),
 *          text            = color.argb("text",            "0xFFFFFFFF");
 *      // spotless:on
 *  }
 * </code>
 * </pre>
 * <p>
 * Then use the color anywhere:
 *
 * <pre>
 * <code>
 *  GuiDraw.drawRect(x, y, w, h, ColorUtils.background.getColor());
 * </code>
 * </pre>
 * <p>
 * Resource packs override colors via a lang file entry: {@code color.resource.mymod.background=80FF20AA}
 * <p>
 * ARGB colors use 8-char hex (AARRGGBB). RGB colors use 6-char hex (RRGGBB), alpha is always FF.
 * <p>
 * Colors are resolved on every resource reload (F3+T) via {@link CacheReloadListener}, registered by GTNHLib's client
 * proxy. On the first reload all instances are scanned to find overrides; subsequent reloads only re-check instances
 * that actually have an override, so mods with no resource pack pay no per-reload cost. Call {@link #invalidate()} to
 * force a full rescan (e.g. after a resource pack is added at runtime).
 */
public class ColorResource {

    private static final Logger LOG = LogManager.getLogger(ColorResource.class);

    // All registered instances. WeakHashMap allows GC if an instance is no longer referenced elsewhere.
    private static final Set<ColorResource> ALL = Collections.newSetFromMap(new WeakHashMap<>());
    // Only instances that have a confirmed resource pack override. Rebuilt on each full scan.
    private static final List<ColorResource> ACTIVE = new ArrayList<>();
    // Guarded by ALL's monitor. False until the first onResourceManagerReload completes.
    private static boolean initialized = false;

    private final String modId;
    private final String name;
    private final int defaultColor;
    private final boolean argb;
    private volatile int cachedColor;

    /**
     * @param modId the mod's assets folder name (e.g. {@code "appliedenergistics2"}, {@code "thaumicenergistics"}) -
     *              must be a plain lowercase string, not a class reference
     * @param name  the color name used in the lang key
     * @param hex   default color - AARRGGBB if {@code argb} is true, RRGGBB otherwise
     * @param argb  true to include the alpha channel, false to force alpha to FF
     */
    public ColorResource(String modId, String name, String hex, boolean argb) {
        this.modId = modId.intern();
        this.name = name;
        this.argb = argb;
        this.defaultColor = parseHex(hex, argb);
        this.cachedColor = this.defaultColor;
        synchronized (ALL) {
            ALL.add(this);
        }
    }

    private static String stripPrefix(String hex) {
        String s = hex.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) return s.substring(2);
        if (s.startsWith("#")) return s.substring(1);
        return s;
    }

    private static int parseHex(String hex, boolean argb) {
        long value = Long.parseLong(stripPrefix(hex), 16);
        return argb ? (int) value : (int) (0xFF000000L | value);
    }

    /** Lang key used to look up a resource pack override, e.g. {@code color.resource.mymod.background}. */
    public String getLangKey() {
        return "color.resource." + modId + "." + name;
    }

    /**
     * Returns the resolved ARGB color value. Checks the resource pack lang file first; falls back to the default color.
     * Updated on every resource reload (F3+T).
     * <p>
     * Example usage:
     *
     * <pre>
     * <code>
     *  GuiDraw.drawRect(x, y, w, h, ColorUtils.background.getColor());
     * </code>
     * </pre>
     */
    public int getColor() {
        return cachedColor;
    }

    private int resolveColor() {
        String langKey = getLangKey();
        if (StatCollector.canTranslate(langKey)) {
            String value = stripPrefix(StatCollector.translateToLocal(langKey));
            try {
                if (!argb && value.length() > 6) {
                    LOG.warn(
                            "Lang key '{}' received ARGB hex '{}' but this color is RGB-only - alpha will be ignored.",
                            langKey,
                            value);
                }
                long parsed = Long.parseLong(value, 16);
                return argb ? (int) parsed : (int) (0xFF000000L | parsed);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid hex '{}' for lang key '{}', using default.", value, langKey);
                return defaultColor;
            }
        }
        return defaultColor;
    }

    /**
     * Forces a full rescan of all registered instances on the next resource reload. Call this if a resource pack is
     * added or removed at runtime so that newly introduced overrides are picked up.
     */
    public static void invalidate() {
        synchronized (ALL) {
            initialized = false;
        }
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
     *      background = colors.argb("background", "0xFF202020"),
     *      text       = colors.rgb("text",        "0xFFFFFF");
     * </code>
     * </pre>
     */
    public static class Factory {

        private final String modId;

        /**
         * @param modId the mod's assets folder name - a plain lowercase string matching the folder under
         *              {@code assets/}, e.g. {@code "appliedenergistics2"}, {@code "blockrenderer6343"},
         *              {@code "bq_standard"}, {@code "hardcoreenderexpansion"}, {@code "thaumicenergistics"}
         */
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

    /**
     * Resolves color values on resource reload (F3+T). Registered by GTNHLib's client proxy.
     * <p>
     * On the first reload all instances are scanned (full scan). After that only instances with a confirmed resource
     * pack override are re-checked, so packs with no overrides pay zero per-reload cost.
     */
    public static class CacheReloadListener implements IResourceManagerReloadListener {

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
            synchronized (ALL) {
                if (!initialized) {
                    fullScan();
                    initialized = true;
                } else {
                    partialScan();
                }
            }
        }

        private static void fullScan() {
            ACTIVE.clear();
            for (ColorResource instance : ALL) {
                int resolved = instance.resolveColor();
                instance.cachedColor = resolved;
                if (resolved != instance.defaultColor) {
                    ACTIVE.add(instance);
                }
            }
        }

        private static void partialScan() {
            // Only re-check instances with a confirmed override. If no resource pack provides overrides,
            // ACTIVE is empty and this loop does nothing.
            ACTIVE.removeIf(instance -> {
                int resolved = instance.resolveColor();
                instance.cachedColor = resolved;
                // Remove from ACTIVE if the override is gone (e.g. resource pack was unloaded)
                return resolved == instance.defaultColor;
            });
        }
    }
}
