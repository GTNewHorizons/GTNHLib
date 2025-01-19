package com.gtnewhorizon.gtnhlib.config;

import java.util.Collection;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PacketSyncConfig implements IMessage {

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
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            syncedElements.put(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(syncedElements.size());
        for (Object2ObjectMap.Entry<String, String> entry : syncedElements.object2ObjectEntrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            ByteBufUtils.writeUTF8String(buf, entry.getValue());
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncConfig, IMessage> {

        @Override
        public IMessage onMessage(PacketSyncConfig message, MessageContext ctx) {
            ConfigSyncHandler.onSync(message);
            return null;
        }
    }
}
