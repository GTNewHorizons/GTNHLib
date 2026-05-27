package com.gtnewhorizon.gtnhlib.teams;

import net.minecraft.nbt.NBTTagCompound;

public interface INetworkTeamData extends ITeamData {

    /**
     * @param tag      The NBTTagCompound to be sent to the client
     * @param complete When true, all data should be added to the tag, not just modified data. This will be true for
     *                 players who logged in or joined the team.
     */
    default void toPacketTag(NBTTagCompound tag, boolean complete) {
        writeToNBT(tag);
    }

    /**
     * @param tag      The NBTTagCompound from the server
     * @param complete When true, the tag contains all data rather than just modified data.
     */
    default void fromPacketTag(NBTTagCompound tag, boolean complete) {
        readFromNBT(tag);
    }

    /**
     * Called after all clients have been synced
     */
    void markSyncedToClient();

}
