package com.gtnewhorizon.gtnhlib.network;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.ClientProxy;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketViewDistance implements IPacket {

    private int viewDistance;

    public PacketViewDistance() {}

    public PacketViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeInt(viewDistance);
    }

    @Override
    public void decode(PacketBuffer buf) {
        viewDistance = buf.readInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        ClientProxy.currentServerViewDistance = viewDistance;
        return null;
    }

}
