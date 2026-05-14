package com.gtnewhorizon.gtnhlib.network;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.util.ClientPlayerUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PlayerDataSyncHandler implements IMessageHandler<PlayerDataSync, IMessage> {

    @Override
    public IMessage onMessage(PlayerDataSync message, MessageContext ctx) {
        GTNHLib.LOG.info("sync arrived with length {}", message.data.size());
        message.data.forEach((name, id) -> GTNHLib.LOG.info("{} {}", name, id));
        synchronized (ClientPlayerUtils.clientPlayerMap) {
            ClientPlayerUtils.clientPlayerMap.clear();
            ClientPlayerUtils.clientPlayerMap.putAll(message.data);
        }
        synchronized (ClientPlayerUtils.clientUsernameCache) {
            ClientPlayerUtils.clientUsernameCache.clear();
            message.data.forEach((name, uuid) -> ClientPlayerUtils.clientUsernameCache.put(uuid, name));
        }
        return null;
    }
}
