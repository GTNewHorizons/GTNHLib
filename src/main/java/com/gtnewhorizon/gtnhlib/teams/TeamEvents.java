package com.gtnewhorizon.gtnhlib.teams;

import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.eventhandler.Event;
import lombok.RequiredArgsConstructor;

public class TeamEvents {

    @RequiredArgsConstructor
    public static class TeamCreateEvent extends Event {

        public final Team team;
        public final UUID owner;

    }

    @RequiredArgsConstructor
    public static class TeamMergeEvent extends Event {

        public final Team consumed;
        public final Team surviving;
    }

    @RequiredArgsConstructor
    public static class TeamLeaveEvent extends Event {

        public final Team team;
        public final UUID player;
        public final Team newTeam;
        public final boolean teamDisbanded;
    }

    @RequiredArgsConstructor
    public static class TeamDisbandEvent extends Event {

        public final Team team;
        public final Map<UUID, Team> newTeamsByMember;
        public final boolean adminAction;
    }

}
