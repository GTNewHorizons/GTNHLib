package com.gtnewhorizon.gtnhlib.teams;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

public interface ITeamData {

    void save(JsonObject obj);

    void load(JsonObject obj);

    /**
     * Called on each piece of ITeamData on the surviving team during a team merge. Implementers are responsible for
     * defining what merging data means for their data type!
     *
     * @param oldTeamData ITeamData of the team that is being disbanded.
     */
    default void mergeData(Team consumed, Team surviving, ITeamData oldTeamData) {}

    default void markDirty() {
        TeamDataSaver.markForSaving();
    }

    /**
     * Called when a player moves from one team to another.
     *
     * @return The copied ITeamData or null if no data should be copied.
     */
    default @Nullable ITeamData copyData(Team oldTeam, Team newTeam, UUID playerId, TeamDataCopyReason reason) {
        return null;
    }

    ITeamData UNIMPLEMENTED = new ITeamData() {

        @Override
        public void save(JsonObject obj) {}

        @Override
        public void load(JsonObject obj) {}
    };
}
