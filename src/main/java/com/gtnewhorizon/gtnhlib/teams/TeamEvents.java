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

    @RequiredArgsConstructor
    public static class TeamJoinEvent extends Event {

        public final Team team;
        public final UUID player;
        public final Team oldTeam;
    }

    @RequiredArgsConstructor
    public static class TeamKickEvent extends Event {

        public final Team team;
        public final UUID kicked;
        public final Team newTeam;
        public final boolean adminAction;
    }

    @RequiredArgsConstructor
    public static class TeamRenameEvent extends Event {

        public final Team team;
        public final String oldName;
        public final String newName;
        public final boolean adminAction;
    }

    @RequiredArgsConstructor
    public static class TeamPromoteEvent extends Event {

        public final Team team;
        public final UUID player;
        public final TeamRole oldRole;
        public final TeamRole newRole;
        public final boolean adminAction;
    }

    @RequiredArgsConstructor
    public static class TeamDemoteEvent extends Event {

        public final Team team;
        public final UUID player;
        public final TeamRole oldRole;
        public final TeamRole newRole;
        public final boolean adminAction;
    }

}
