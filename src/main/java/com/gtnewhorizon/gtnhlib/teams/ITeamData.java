package com.gtnewhorizon.gtnhlib.teams;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

public interface ITeamData {

    void writeToNBT(NBTTagCompound tag);

    void readFromNBT(NBTTagCompound tag);

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
        public void writeToNBT(NBTTagCompound tag) {}

        @Override
        public void readFromNBT(NBTTagCompound tag) {}
    };
}
