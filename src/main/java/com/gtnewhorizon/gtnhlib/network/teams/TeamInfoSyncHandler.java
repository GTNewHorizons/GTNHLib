package com.gtnewhorizon.gtnhlib.network.teams;

import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class TeamInfoSyncHandler implements IMessageHandler<TeamInfoSync, IMessage> {

    @Override
    public IMessage onMessage(TeamInfoSync message, MessageContext ctx) {
        TeamManagerClient.onTeamInfoSyncPacket(message);
        return null;
    }

}
