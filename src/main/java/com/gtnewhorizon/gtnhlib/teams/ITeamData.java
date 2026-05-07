package com.gtnewhorizon.gtnhlib.teams;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

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

    /**
     * Called when a player moves from one team to another.
     *
     * @param playerId     UUID of the player who is moving teams
     * @param prevTeamData ITeamData of the previous team
     */
    default void copyData(Team prevTeam, Team newTeam, UUID playerId, ITeamData prevTeamData,
            TeamDataCopyReason reason) {}

    ITeamData UNIMPLEMENTED = new ITeamData() {

        @Override
        public void writeToNBT(NBTTagCompound tag) {}

        @Override
        public void readFromNBT(NBTTagCompound tag) {}
    };
}
