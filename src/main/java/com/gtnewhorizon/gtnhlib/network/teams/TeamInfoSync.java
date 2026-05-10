package com.gtnewhorizon.gtnhlib.network.teams;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkUtils;
import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TeamInfoSync implements IPacket {

    public UUID uuid;
    public String name;

    public TeamInfoSync() {}

    public TeamInfoSync(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        NetworkUtils.writeUUID(buf, uuid);
        buf.writeStringToBuffer(name);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        uuid = NetworkUtils.readUUID(buf);
        name = buf.readStringFromBuffer(NetworkUtils.MAX_STRING_BYTES);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        TeamManagerClient.onTeamInfoSyncPacket(this);
        return null;
    }

}
