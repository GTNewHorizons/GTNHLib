package com.gtnewhorizon.gtnhlib.compat;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

/**
 * Isolated, reflection-based access to the Baubles inventory so the library never hard-depends on Baubles. Only
 * touched when {@link Mods#BAUBLES} is true. {@code baubles.api.BaublesApi.getBaubles(EntityPlayer)} returns the
 * full (possibly expanded) bauble inventory.
 */
public final class BaublesCompat {

    private static Method getBaublesMethod;
    private static volatile boolean resolved;

    private BaublesCompat() {}

    public static IInventory getBaubles(EntityPlayer player) {
        if (!resolved) resolve();
        if (getBaublesMethod == null) return null;
        try {
            return (IInventory) getBaublesMethod.invoke(null, player);
        } catch (Throwable t) {
            return null;
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        try {
            final Class<?> api = Class.forName("baubles.api.BaublesApi");
            getBaublesMethod = api.getMethod("getBaubles", EntityPlayer.class);
        } catch (Throwable t) {
            getBaublesMethod = null;
        }
        resolved = true;
    }
}
