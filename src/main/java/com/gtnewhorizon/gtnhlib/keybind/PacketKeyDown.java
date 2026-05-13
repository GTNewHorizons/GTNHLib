package com.gtnewhorizon.gtnhlib.keybind;

import java.io.IOException;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

public class PacketKeyDown implements IPacket {

    private Int2BooleanMap updateKeys;

    @SuppressWarnings("unused")
    public PacketKeyDown() {}

    protected PacketKeyDown(Int2BooleanMap updateKeys) {
        this.updateKeys = updateKeys;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(updateKeys.size());
        for (var entry : updateKeys.int2BooleanEntrySet()) {
            buf.writeInt(entry.getIntKey());
            buf.writeBoolean(entry.getBooleanValue());
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        updateKeys = new Int2BooleanOpenHashMap();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            updateKeys.put(buf.readInt(), buf.readBoolean());
        }
    }

    @Override
    public IPacket executeServer(NetHandlerPlayServer handler) {
        for (var entry : updateKeys.int2BooleanEntrySet()) {
            SyncedKeybind keybind = SyncedKeybind.getFromSyncId(entry.getIntKey());
            keybind.serverActivate(entry.getBooleanValue(), handler.playerEntity);
        }
        return null;
    }
}
