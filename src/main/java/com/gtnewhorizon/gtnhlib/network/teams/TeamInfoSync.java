package com.gtnewhorizon.gtnhlib.network.teams;

import java.util.UUID;

import com.gtnewhorizon.gtnhlib.teams.TeamRole;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class TeamInfoSync implements IMessage {

    public UUID uuid;
    public String name;
    public TeamRole role;

    public TeamInfoSync() {}

    public TeamInfoSync(UUID uuid, String name, TeamRole role) {
        this.uuid = uuid;
        this.name = name;
        this.role = role;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeShort((short) role.ordinal());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        uuid = new UUID(buf.readLong(), buf.readLong());
        name = ByteBufUtils.readUTF8String(buf);
        role = TeamRole.values()[buf.readShort()];
    }
}
