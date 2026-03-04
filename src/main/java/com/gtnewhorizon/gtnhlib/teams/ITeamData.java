package com.gtnewhorizon.gtnhlib.teams;

import net.minecraft.nbt.NBTTagCompound;

public interface ITeamData {

    void writeToNBT(NBTTagCompound NBT);

    void readFromNBT(NBTTagCompound NBT);

    /**
     * Called on each piece of ITeamData on the surviving team during a team merge. Implementers are responsible for
     * defining what merging data means for their data type!
     * 
     * @param data ITeamData of the team that is being disbanded.
     */
    default void mergeTeams(ITeamData data) {}

    default void markDirty() {
        TeamWorldSavedData.markForSaving();
    }

    ITeamData UNIMPLEMENTED = new ITeamData() {

        @Override
        public void writeToNBT(NBTTagCompound NBT) {}

        @Override
        public void readFromNBT(NBTTagCompound NBT) {}
    };
}
