package com.gtnewhorizon.gtnhlib.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;

public class ResourceUtil {

    private static abstract class SidedResourceLoader {

        // 14810 (max size GT5 as of 2026/29/5) * 1.333 = 19.7k ~= 20k
        private static final int EXISTS_CACHE_SIZE = 20_000;
        private Object2BooleanOpenHashMap<ResourceLocation> EXISTS_CACHE = new Object2BooleanOpenHashMap<>(
                EXISTS_CACHE_SIZE);

        @NotNull
        abstract InputStream getInputStream(@NotNull ResourceLocation resLoc) throws IOException;

        public boolean resourceExists(@NotNull ResourceLocation resLoc) {
            return EXISTS_CACHE.computeIfAbsent(resLoc, this::checkResource);
        }

        abstract boolean checkResource(@NotNull ResourceLocation resLoc);

        public void clearCache() {
            EXISTS_CACHE = new Object2BooleanOpenHashMap<>(EXISTS_CACHE_SIZE);
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ClientResourceLoader extends SidedResourceLoader {

        private static final ClientResourceLoader INSTANCE = new ClientResourceLoader();

        private ClientResourceLoader() {}

        @Override
        public @NotNull InputStream getInputStream(@NotNull ResourceLocation resLoc) throws IOException {
            IResourceManager manager = Minecraft.getMinecraft().getResourceManager();

            IResource resource = manager.getResource(resLoc);

            return resource.getInputStream();
        }

        @Override
        public boolean checkResource(@NotNull ResourceLocation resLoc) {

            final IResourceManager resMan = Minecraft.getMinecraft().getResourceManager();
            if (resMan instanceof SimpleReloadableResourceManager simple) {
                FallbackResourceManager fallback = simple.domainResourceManagers.get(resLoc.getResourceDomain());
                if (fallback != null) {
                    for (IResourcePack rp : fallback.resourcePacks) {
                        if (rp != null && rp.resourceExists(resLoc)) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            // Fallback in case some mod changed how the resource manager stores ResourcePacks
            IResource res = null;
            try {
                res = resMan.getResource(resLoc);
                return true;
            } catch (IOException ignored) {
                return false;
            } finally {
                if (res != null) {
                    try {
                        InputStream stream = res.getInputStream();
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    private static class JarResourceLoader extends SidedResourceLoader {

        private static final ClientResourceLoader INSTANCE = new ClientResourceLoader();

        private JarResourceLoader() {}

        @Override
        public @NotNull InputStream getInputStream(@NotNull ResourceLocation resLoc) throws IOException {
            String path = getAssetPath(resLoc);
            InputStream is = getClassLoader().getResourceAsStream(path);
            if (is == null) {
                throw new FileNotFoundException("Could not find jar asset " + path);
            }
            return is;
        }

        private static ClassLoader getClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }

        private static String getAssetPath(ResourceLocation resLoc) {
            return "assets/" + resLoc.getResourceDomain() + "/" + resLoc.getResourcePath();
        }

        @Override
        protected boolean checkResource(@NotNull ResourceLocation resLoc) {
            String path = getAssetPath(resLoc);
            return getClassLoader().getResource(path) == null;
        }
    }

    private final static ThreadLocal<SidedResourceLoader> SIDED_RESOURCE_LOADER = ThreadLocal.withInitial(() -> {
        try {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                return ClientResourceLoader.INSTANCE;
            }
        } catch (Throwable err) {
            // if we're in a test environment the FMLCommonHandler.getInstance() call will error out. so fallback
            // to loading from jar resources when that happens. Empty on purpose.
        }
        return JarResourceLoader.INSTANCE;
    });

    /**
     * Checks whether a resource exists.
     *
     * @param resLoc the {@link ResourceLocation} identifying the resource to check
     * @return {@code true} if the resource exists; {@code false} otherwise
     */
    public static boolean resourceExists(@NotNull ResourceLocation resLoc) {
        return SIDED_RESOURCE_LOADER.get().resourceExists(resLoc);
    }

    @ApiStatus.Internal
    public static void clearCache() {
        SIDED_RESOURCE_LOADER.get().clearCache();
    }

    /**
     * Get the complete resource location from the short local resource location key.
     *
     * @param basePath    The base path for the resource (e.g.: textures/blocks/)
     * @param ext         The file name extension of the resource (e.g.: .png)
     * @param resourceKey The local resource location key (without textures/blocks|items and without the filename
     *                    extension)
     * @return The complete ResourceLocation pointing to the resource file
     */
    public static @NotNull ResourceLocation getCompleteResourceLocation(@NotNull String basePath, @NotNull String ext,
            @NotNull String resourceKey) {
        final int i = resourceKey.indexOf(':');
        final String domain = (i <= 0) ? "" : resourceKey.substring(0, i);
        final String path = (i < 0) ? resourceKey : resourceKey.substring(i + 1);
        return new ResourceLocation(domain, basePath + path + ext);
    }

    public static @NotNull ResourceLocation getCompleteBlockTextureResourceLocation(@NotNull String resourceKey) {
        return getCompleteResourceLocation("textures/blocks/", ".png", resourceKey);
    }

    public static @NotNull ResourceLocation getCompleteItemTextureResourceLocation(@NotNull String resourceKey) {
        return getCompleteResourceLocation("textures/items/", ".png", resourceKey);
    }

    public static List<String> readLinesFrom(@NotNull ResourceLocation loc) throws IOException {
        try (InputStream is = SIDED_RESOURCE_LOADER.get().getInputStream(loc)) {
            return IOUtils.readLines(is, StandardCharsets.UTF_8);
        }
    }

    public static @NotNull InputStream getInputStream(@NotNull ResourceLocation loc) throws IOException {
        return SIDED_RESOURCE_LOADER.get().getInputStream(loc);
    }

}
