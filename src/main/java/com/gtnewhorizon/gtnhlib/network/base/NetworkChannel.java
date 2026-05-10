package com.gtnewhorizon.gtnhlib.network.base;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import lombok.Getter;

public class NetworkChannel {

    @Getter
    private final SimpleNetworkWrapper network;
    private final AtomicInteger nextId = new AtomicInteger();

    public NetworkChannel(String channel) {
        this.network = NetworkRegistry.INSTANCE.newSimpleChannel(channel);
    }

    // Packets the client receives — sent from server to client.
    public void toClient(Class<? extends IPacket> packet) {
        register(packet, Side.CLIENT);
    }

    // Packets the server receives — sent from client to server.
    public void toServer(Class<? extends IPacket> packet) {
        register(packet, Side.SERVER);
    }

    private void register(Class<? extends IPacket> packet, Side side) {
        network.registerMessage((Class) packet, (Class) packet, nextId.getAndIncrement(), side);
    }

    // Use where a Packet object is needed, e.g. TileEntity#getDescriptionPacket.
    public Packet getPacketFrom(IPacket packet) {
        return network.getPacketFrom(packet);
    }

    // Client -> server.
    public void sendToServer(IPacket packet) {
        network.sendToServer(packet);
    }

    // Server -> specific player.
    public void sendTo(IPacket packet, EntityPlayerMP player) {
        network.sendTo(packet, player);
    }

    // Server -> all connected players.
    public void sendToAll(IPacket packet) {
        network.sendToAll(packet);
    }

    // Server -> all players within range of a point.
    public void sendToAllAround(IPacket packet, NetworkRegistry.TargetPoint point) {
        network.sendToAllAround(packet, point);
    }

    // Server -> all players in a dimension.
    public void sendToDimension(IPacket packet, int dimensionId) {
        network.sendToDimension(packet, dimensionId);
    }

    // Server -> all players tracking an entity.
    public void sendToAllTracking(IPacket packet, Entity entity) {
        if (!(entity.worldObj instanceof WorldServer worldServer)) return;
        worldServer.getEntityTracker().func_151247_a(entity, network.getPacketFrom(packet));
    }

    // Server -> all players tracking an entity, including the entity itself if it's a player.
    public void sendToAllTrackingAndSelf(IPacket packet, Entity entity) {
        if (!(entity.worldObj instanceof WorldServer worldServer)) return;
        worldServer.getEntityTracker().func_151248_b(entity, network.getPacketFrom(packet));
    }

}
