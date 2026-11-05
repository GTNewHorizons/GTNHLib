package com.gtnewhorizon.gtnhlib.network.payload;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;

/** Serverbound payloads must be under 32767 bytes, clientbound under about 2 MB. */
public final class PacketDistributor {

    private PacketDistributor() {}

    public static void sendToServer(CustomPacketPayload payload) {
        PayloadChannel.of(payload).sendToServer(payload);
    }

    /** No-op for players without a connection, such as fake players. */
    public static void sendToPlayer(EntityPlayerMP player, CustomPacketPayload payload) {
        if (player.playerNetServerHandler != null) {
            player.playerNetServerHandler.sendPacket(toClient(payload));
        }
    }

    public static void sendToPlayersInDimension(WorldServer level, CustomPacketPayload payload) {
        players().sendPacketToAllPlayersInDimension(toClient(payload), level.provider.dimensionId);
    }

    public static void sendToPlayersNear(WorldServer level, @Nullable EntityPlayerMP excluded, double x, double y,
            double z, double radius, CustomPacketPayload payload) {
        players().sendToAllNearExcept(excluded, x, y, z, radius, level.provider.dimensionId, toClient(payload));
    }

    public static void sendToAllPlayers(CustomPacketPayload payload) {
        players().sendPacketToAllPlayers(toClient(payload));
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload) {
        if (entity.worldObj instanceof WorldServer level) {
            level.getEntityTracker().func_151247_a(entity, toClient(payload));
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload) {
        if (entity.worldObj instanceof WorldServer level) {
            level.getEntityTracker().func_151248_b(entity, toClient(payload));
        }
    }

    public static void sendToPlayersTrackingChunk(WorldServer level, ChunkCoordIntPair chunk,
            CustomPacketPayload payload) {
        Packet packet = toClient(payload);
        for (Object entry : level.playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) entry;
            if (player.playerNetServerHandler != null
                    && level.getPlayerManager().isPlayerWatchingChunk(player, chunk.chunkXPos, chunk.chunkZPos)) {
                player.playerNetServerHandler.sendPacket(packet);
            }
        }
    }

    private static Packet toClient(CustomPacketPayload payload) {
        return PayloadChannel.of(payload).packet(payload, Side.CLIENT);
    }

    private static ServerConfigurationManager players() {
        return MinecraftServer.getServer().getConfigurationManager();
    }
}
