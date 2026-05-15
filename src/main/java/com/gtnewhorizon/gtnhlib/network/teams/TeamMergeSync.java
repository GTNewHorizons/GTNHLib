package com.gtnewhorizon.gtnhlib.network.teams;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class TeamMergeSync implements IMessage {

    public Set<UUID> incoming = new HashSet<>(); // Teams asking to be consumed by you
    public Set<UUID> outgoing = new HashSet<>(); // Teams you asked to consume your team

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(incoming.size());
        incoming.forEach(uuid -> {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        });
        buf.writeInt(outgoing.size());
        outgoing.forEach(uuid -> {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        });
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
            outgoing.add(new UUID(buf.readLong(), buf.readLong()));
        }
    }

}
