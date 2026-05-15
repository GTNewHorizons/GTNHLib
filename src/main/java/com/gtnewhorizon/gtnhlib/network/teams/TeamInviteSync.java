package com.gtnewhorizon.gtnhlib.network.teams;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class TeamInviteSync implements IMessage {

    public Set<UUID> incoming = new HashSet<>(); // Teams inviting you to join
    public Set<String> outgoing = new HashSet<>(); // Players you have invited to join

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(incoming.size());
        incoming.forEach(uuid -> {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        });
        buf.writeInt(outgoing.size());
        outgoing.forEach(name -> ByteBufUtils.writeUTF8String(buf, name));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        incoming.clear();
        outgoing.clear();

        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            incoming.add(new UUID(buf.readLong(), buf.readLong()));
        }
        size = buf.readInt();
        for (int i = 0; i < size; i++) {
            outgoing.add(ByteBufUtils.readUTF8String(buf));
        }
    }

}
