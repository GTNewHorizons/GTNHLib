package com.gtnewhorizon.gtnhlib.network.teams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkUtils;
import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TeamDataSync implements IPacket {

    public boolean complete;
    public List<Pair<String, NBTTagCompound>> data;

    public TeamDataSync() {}

    public TeamDataSync(List<Pair<String, NBTTagCompound>> data) {
        this.complete = true;
        this.data = data;
    }

    public TeamDataSync(String key, NBTTagCompound tag) {
        this.complete = false;
        this.data = Collections.singletonList(Pair.of(key, tag));
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeBoolean(complete);
        buf.writeShort(data.size());
        for (Pair<String, NBTTagCompound> pair : data) {
            buf.writeStringToBuffer(pair.getLeft());
            buf.writeNBTTagCompoundToBuffer(pair.getRight());
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        complete = buf.readBoolean();
        int length = buf.readShort();
        data = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            String key = buf.readStringFromBuffer(NetworkUtils.MAX_STRING_BYTES);
            NBTTagCompound tag = buf.readNBTTagCompoundFromBuffer();
            data.add(Pair.of(key, tag));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        TeamManagerClient.onTeamDataSyncPacket(this);
        return null;
    }

}
