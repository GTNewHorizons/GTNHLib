package com.gtnewhorizon.gtnhlib.network.teams;

import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class TeamMergeSyncHandler implements IMessageHandler<TeamMergeSync, IMessage> {

    @Override
    public IMessage onMessage(TeamMergeSync message, MessageContext ctx) {
        TeamManagerClient.onTeamMergeSyncPacket(message);
        return null;
    }
}
