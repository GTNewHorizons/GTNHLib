package com.gtnewhorizon.gtnhlib.teams;

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

}
