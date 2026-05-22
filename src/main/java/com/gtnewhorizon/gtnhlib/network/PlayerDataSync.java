package com.gtnewhorizon.gtnhlib.network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkUtils;
import com.gtnewhorizon.gtnhlib.util.ClientPlayerUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PlayerDataSync implements IPacket {

    public Map<String, UUID> data = new HashMap<>();

    public PlayerDataSync() {}

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(data.size());
        data.forEach((name, uuid) -> {
            NetworkUtils.writeString(buf, name);
            NetworkUtils.writeUUID(buf, uuid);
        });
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        int length = buf.readInt();
        for (int i = 0; i < length; i++) {
            data.put(NetworkUtils.readString(buf), NetworkUtils.readUUID(buf));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        synchronized (ClientPlayerUtils.clientPlayerMap) {
            ClientPlayerUtils.clientPlayerMap.clear();
            ClientPlayerUtils.clientPlayerMap.putAll(data);
        }
        synchronized (ClientPlayerUtils.clientUsernameCache) {
            ClientPlayerUtils.clientUsernameCache.clear();
            data.forEach((name, uuid) -> ClientPlayerUtils.clientUsernameCache.put(uuid, name));
        }
        return null;
    }
}
