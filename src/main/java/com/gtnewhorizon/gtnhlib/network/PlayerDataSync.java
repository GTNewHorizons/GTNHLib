package com.gtnewhorizon.gtnhlib.network;

import java.util.HashSet;
import java.util.Set;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PlayerDataSync implements IMessage {

    public Set<String> data = new HashSet<>();

    public PlayerDataSync() {}

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(data.size());
        data.forEach(name -> ByteBufUtils.writeUTF8String(buf, name));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int length = buf.readInt();
        for (int i = 0; i < length; i++) {
            data.add(ByteBufUtils.readUTF8String(buf));
        }
    }

}
