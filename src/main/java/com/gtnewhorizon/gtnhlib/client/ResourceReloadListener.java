package com.gtnewhorizon.gtnhlib.client;

import java.util.LinkedList;
import java.util.function.Consumer;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ResourceReloadListener implements IResourceManagerReloadListener {

    @ApiStatus.Internal
    public static final IResourceManagerReloadListener INSTANCE = new ResourceReloadListener();
    private static final @NotNull LinkedList<@NotNull Consumer<IResourceManager>> callbacks = new LinkedList<>();

    private ResourceReloadListener() {}

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        synchronized (callbacks) {
            // run the callbacks that pre-cache formatting to an even higher degree.
            for (Consumer<IResourceManager> callback : callbacks) {
                callback.accept(resourceManager);
            }
        }
    }

    public static void registerEarlyCallback(@NotNull Consumer<IResourceManager> callback) {
        synchronized (callbacks) {
            callbacks.addFirst(callback);
        }
    }

    public static void registerLateCallback(@NotNull Consumer<IResourceManager> callback) {
        synchronized (callbacks) {
            callbacks.addLast(callback);
        }
    }
}
