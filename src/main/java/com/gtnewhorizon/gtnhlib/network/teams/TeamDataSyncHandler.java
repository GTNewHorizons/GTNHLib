package com.gtnewhorizon.gtnhlib.network.teams;

import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class TeamDataSyncHandler implements IMessageHandler<TeamDataSync, IMessage> {

    @Override
    public IMessage onMessage(TeamDataSync message, MessageContext ctx) {
        TeamManagerClient.onTeamDataSyncPacket(message);
        return null;
    }

}
