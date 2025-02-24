package com.gtnewhorizon.gtnhlib.network;

import com.gtnewhorizon.gtnhlib.ClientProxy;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketViewDistance implements IMessage {

    private int viewDistance;

    public PacketViewDistance() {}

    public PacketViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        viewDistance = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(viewDistance);
    }

    public static class Handler implements IMessageHandler<PacketViewDistance, IMessage> {

        @Override
        public IMessage onMessage(PacketViewDistance message, MessageContext ctx) {
            ClientProxy.currentServerViewDistance = message.viewDistance;
            return null;
        }
    }
}
