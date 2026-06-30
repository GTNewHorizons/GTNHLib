package com.gtnewhorizon.gtnhlib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Isolates the client-only {@link Minecraft} reference so it never loads on a dedicated server. */
@SideOnly(Side.CLIENT)
public final class PlayerInventoryClientHelper {

    private PlayerInventoryClientHelper() {}

    public static boolean isLocalPlayer(EntityPlayer player) {
        return player == Minecraft.getMinecraft().thePlayer;
    }
}
