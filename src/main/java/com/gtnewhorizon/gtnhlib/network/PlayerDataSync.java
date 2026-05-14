package com.gtnewhorizon.gtnhlib.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PlayerDataSync implements IMessage {

    public Map<String, UUID> data = new HashMap<>();

    public PlayerDataSync() {}

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(data.size());
        data.forEach((name, uuid) -> {
            ByteBufUtils.writeUTF8String(buf, name);
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        });
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int length = buf.readInt();
        for (int i = 0; i < length; i++) {
            data.put(ByteBufUtils.readUTF8String(buf), new UUID(buf.readLong(), buf.readLong()));
        }
    }

}
