package com.gtnewhorizon.gtnhlib.keybind;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

public class PacketKeyDown implements IMessage {

    private Int2BooleanMap updateKeys;

    @SuppressWarnings("unused")
    public PacketKeyDown() {}

    protected PacketKeyDown(Int2BooleanMap updateKeys) {
        this.updateKeys = updateKeys;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.updateKeys = new Int2BooleanOpenHashMap();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            updateKeys.put(buf.readInt(), buf.readBoolean());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(updateKeys.size());
        for (var entry : updateKeys.int2BooleanEntrySet()) {
            buf.writeInt(entry.getIntKey());
            buf.writeBoolean(entry.getBooleanValue());
        }
    }

    public static class HandlerKeyDown implements IMessageHandler<PacketKeyDown, IMessage> {

        @Override
        public IMessage onMessage(PacketKeyDown message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                for (var entry : message.updateKeys.int2BooleanEntrySet()) {
                    SyncedKeybind keybind = SyncedKeybind.getFromSyncId(entry.getIntKey());
                    keybind.serverActivate(entry.getBooleanValue(), ctx.getServerHandler().playerEntity);
                }
            }
            return null;
        }
    }
}
