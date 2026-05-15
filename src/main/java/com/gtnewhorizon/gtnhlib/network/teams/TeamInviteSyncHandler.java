package com.gtnewhorizon.gtnhlib.network.teams;

import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class TeamInviteSyncHandler implements IMessageHandler<TeamInviteSync, IMessage> {

    @Override
    public IMessage onMessage(TeamInviteSync message, MessageContext ctx) {
        TeamManagerClient.onTeamPlayerInviteSyncPacket(message);
        return null;
    }
}
