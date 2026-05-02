package com.gtnewhorizon.gtnhlib.teams;

import net.minecraft.nbt.NBTTagCompound;

public interface INetworkTeamData extends ITeamData {

    default void toPacketTag(NBTTagCompound tag, boolean complete) {
        writeToNBT(tag);
    }

    default void fromPacketTag(NBTTagCompound tag, boolean complete) {
        readFromNBT(tag);
    }

    boolean shouldSyncToClient();

    void markSyncedToClient();

}
