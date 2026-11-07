package com.gtnewhorizon.gtnhlib.structure.nbt;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * Holds the data text of a {@link BlockStructureMarker}, stored like a modern structure block in data mode.
 */
public class TileEntityStructureMarker extends TileEntity {

    public static final String TAG_DATA = "metadata";

    private String data = "";

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data == null ? "" : data;
        this.markDirty();
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.data = tag.getString(TAG_DATA);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setString(TAG_DATA, this.data);
    }
}
