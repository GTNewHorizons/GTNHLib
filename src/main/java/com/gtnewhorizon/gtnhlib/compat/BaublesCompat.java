package com.gtnewhorizon.gtnhlib.compat;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

/**
 * Reflection-based access to Baubles inventory (no hard dependency). Lazy-init with double-checked locking.
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

    // classload attempt is fine since its only done once
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
