package com.gtnewhorizon.gtnhlib.network.teams;

import java.util.UUID;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class TeamInfoSync implements IMessage {

    public UUID uuid;
    public String name;

    public TeamInfoSync() {}

    public TeamInfoSync(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        ByteBufUtils.writeUTF8String(buf, name);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        uuid = new UUID(buf.readLong(), buf.readLong());
        name = ByteBufUtils.readUTF8String(buf);
    }

}
