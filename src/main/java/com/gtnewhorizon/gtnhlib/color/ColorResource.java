package com.gtnewhorizon.gtnhlib.color;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.GTNHLib;

/**
 * An ARGB color constant with resource pack override and caching support.
 * <p>
 * Declare public static final fields in any class; the field name and declaring class are discovered automatically:
 *
 * <pre>
 * <code>
 * &#64;ColorResource.Mod("mymod")
 * public class MyColors {
 *     public static final ColorResource
 *         background = new ColorResource("FF202020"),
 *         blue       = new ColorResource("FF0000FF"),
 *         title      = new ColorResource("FFFFFFFF");
 * }
 * </code>
 * </pre>
 *
 * <p>
 * Resource packs override colors via a lang file entry (AARRGGBB hex, no prefix):
 * {@code gui.color.mymod.background=80FF20AA}
 * <p>
 * The color cache is cleared automatically on F3+T via {@link CacheReloadListener}, registered by GTNHLib's client
 * proxy.
 */
public class ColorResource {

    /** Sets the namespace used in the lang key. Defaults to the declaring class's simple name. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Mod {

        String value();
    }

    private static final Set<ColorResource> INSTANCES = Collections
            .synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final int defaultColor;
    private final Class<?> declaringClass;
    private volatile String langKey;
    private volatile long cachedColor = -1L;

    public ColorResource(String hex) {
        int parsed;
        try {
            parsed = (int) Long.parseLong(hex.trim(), 16);
        } catch (NumberFormatException e) {
            GTNHLib.LOG.warn("[ColorResource] Invalid default hex '{}', using opaque white.", hex);
            parsed = 0xFFFFFFFF;
        }
        this.defaultColor = parsed;
        this.declaringClass = captureDeclaringClass();
        INSTANCES.add(this);
    }

    private static Class<?> captureDeclaringClass() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if ("<clinit>".equals(frame.getMethodName())) {
                try {
                    return Class.forName(frame.getClassName());
                } catch (ClassNotFoundException ignored) {}
            }
        }
        return null;
    }

    /** Returns the mod namespace from {@link Mod} if present, otherwise the declaring class's simple name. */
    public String getModId() {
        if (declaringClass == null) return "unknown";
        Mod mod = declaringClass.getAnnotation(Mod.class);
        return mod != null ? mod.value() : declaringClass.getSimpleName();
    }

    /** Lang key used to look up a resource pack override. */
    public String getLangKey() {
        if (langKey != null) return langKey;
        if (declaringClass == null) {
            GTNHLib.LOG.warn("[ColorResource] Could not determine declaring class for a ColorResource instance.");
            return langKey = "gui.color.unknown.unknown";
        }
        String name = "unknown";
        for (Field f : declaringClass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != ColorResource.class) continue;
            f.setAccessible(true);
            try {
                if (f.get(null) == this) {
                    name = f.getName();
                    break;
                }
            } catch (IllegalAccessException ignored) {}
        }
        if ("unknown".equals(name)) {
            GTNHLib.LOG.warn("[ColorResource] Could not resolve field name in {}.", declaringClass.getSimpleName());
        }
        return langKey = "gui.color." + getModId() + "." + name;
    }

    /**
     * Returns the resolved ARGB color value. Checks the resource pack lang file first; falls back to the default color.
     * Result is cached until the next resource reload (F3+T).
     * <p>
     * Example usage:
     * 
     * <pre>
     * <code>
     * GuiDraw.drawRect(x, y, w, h, MyColors.background.getColor());
     * </code>
     * </pre>
     */
    public int getColor() {
        if (cachedColor != -1L) return (int) cachedColor;

        String key = getLangKey();
        int color;
        if (StatCollector.canTranslate(key)) {
            String value = StatCollector.translateToLocal(key).trim();
            try {
                color = (int) Long.parseLong(value, 16);
            } catch (NumberFormatException e) {
                GTNHLib.LOG.warn("[ColorResource] Invalid hex '{}' for lang key '{}', using default.", value, key);
                color = defaultColor;
            }
        } else {
            color = defaultColor;
        }

        cachedColor = color;
        return color;
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
