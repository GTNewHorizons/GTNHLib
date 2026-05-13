package com.gtnewhorizon.gtnhlib.network.base;

import java.io.IOException;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public interface IPacket extends IMessage, IMessageHandler<IPacket, IMessage> {

    void encode(PacketBuffer buf) throws IOException;

    void decode(PacketBuffer buf) throws IOException;

    @Override
    default IPacket onMessage(IPacket message, MessageContext ctx) {
        return switch (ctx.side) {
            case CLIENT -> message.executeClient(ctx.getClientHandler());
            case SERVER -> message.executeServer(ctx.getServerHandler());
        };
    }

    @Override
    default void fromBytes(ByteBuf buf) {
        try {
            decode(new PacketBuffer(buf));
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode packet " + getClass().getSimpleName(), e);
        }
    }

    @Override
    default void toBytes(ByteBuf buf) {
        try {
            encode(new PacketBuffer(buf));
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode packet " + getClass().getSimpleName(), e);
        }
    }

    // Called on the client when this packet is received.
    @SideOnly(Side.CLIENT)
    default IPacket executeClient(NetHandlerPlayClient handler) {
        return null;
    }

    // Called on the server when this packet is received.
    default IPacket executeServer(NetHandlerPlayServer handler) {
        return null;
    }
}
