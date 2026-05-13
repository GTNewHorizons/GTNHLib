package com.gtnewhorizon.gtnhlib.config;

import java.io.IOException;
import java.util.Collection;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PacketSyncConfig implements IPacket {

    public final Object2ObjectMap<String, String> syncedElements = new Object2ObjectOpenHashMap<>();

    @SuppressWarnings("unused")
    public PacketSyncConfig() {}

    PacketSyncConfig(Collection<SyncedConfigElement> elements) {
        for (SyncedConfigElement element : elements) {
            try {
                syncedElements.put(element.toString(), element.getValue());
            } catch (ConfigException e) {
                GTNHLib.LOG.error("Failed to serialize config element: {}", element.toString(), e);
            }
        }
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(syncedElements.size());
        for (Object2ObjectMap.Entry<String, String> entry : syncedElements.object2ObjectEntrySet()) {
            buf.writeStringToBuffer(entry.getKey());
            buf.writeStringToBuffer(entry.getValue());
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            syncedElements.put(
                    buf.readStringFromBuffer(NetworkUtils.MAX_STRING_BYTES),
                    buf.readStringFromBuffer(NetworkUtils.MAX_STRING_BYTES));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        ConfigSyncHandler.onSync(this);
        return null;
    }
}
