package com.gtnewhorizon.gtnhlib.keybind;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class PacketKeyPressed implements IMessage {

    private IntList pressedKeys;

    @SuppressWarnings("unused")
    public PacketKeyPressed() {}

    protected PacketKeyPressed(IntList pressedKeys) {
        this.pressedKeys = pressedKeys;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pressedKeys = new IntArrayList();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            pressedKeys.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pressedKeys.size());
        for (int key : pressedKeys) {
            buf.writeInt(key);
        }
    }

    public static class HandlerKeyPressed implements IMessageHandler<PacketKeyPressed, IMessage> {

        @Override
        public IMessage onMessage(PacketKeyPressed message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                for (int index : message.pressedKeys) {
                    SyncedKeybind keybind = SyncedKeybind.getFromSyncId(index);
                    keybind.onKeyPressed(ctx.getServerHandler().playerEntity);
                }
            }
            return null;
        }
    }
}
