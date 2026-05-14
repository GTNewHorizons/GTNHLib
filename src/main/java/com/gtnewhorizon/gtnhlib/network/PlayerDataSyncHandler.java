package com.gtnewhorizon.gtnhlib.network;

import com.gtnewhorizon.gtnhlib.util.ClientPlayerUtils;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class PlayerDataSyncHandler implements IMessageHandler<PlayerDataSync, IMessage> {

    @Override
    public IMessage onMessage(PlayerDataSync message, MessageContext ctx) {
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
