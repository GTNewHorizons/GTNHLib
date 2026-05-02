package com.gtnewhorizon.gtnhlib.teams;

import com.gtnewhorizon.gtnhlib.network.teams.TeamInfoSync;

public class TeamNetwork {

    protected static TeamInfoSync CreateTeamInfoSyncPacket(Team team) {
        return new TeamInfoSync(team.getTeamId(), team.getTeamName());
    }

}
