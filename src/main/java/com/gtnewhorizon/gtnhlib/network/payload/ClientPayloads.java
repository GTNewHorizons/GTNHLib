package com.gtnewhorizon.gtnhlib.network.payload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class ClientPayloads {

    private ClientPayloads() {}

    static EntityPlayer player() {
        return Minecraft.getMinecraft().thePlayer;
    }

    static boolean onMainThread() {
        return Minecraft.getMinecraft().func_152345_ab();
    }

    static void schedule(Runnable task) {
        Minecraft.getMinecraft().func_152344_a(task);
    }

    static void disconnect(String reason) {
        NetHandlerPlayClient handler = Minecraft.getMinecraft().getNetHandler();
        if (handler != null) {
            handler.getNetworkManager().closeChannel(new ChatComponentText(reason));
        }
    }
}
