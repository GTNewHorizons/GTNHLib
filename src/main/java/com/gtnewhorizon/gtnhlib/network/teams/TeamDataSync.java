package com.gtnewhorizon.gtnhlib.network.teams;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.apache.commons.lang3.tuple.Pair;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class TeamDataSync implements IMessage {

    private List<Pair<String, NBTTagCompound>> data;

    public TeamDataSync() {}

    public TeamDataSync(List<Pair<String, NBTTagCompound>> data) {
        this.data = data;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(data.size());
        for (Pair<String, NBTTagCompound> pair : data) {
            ByteBufUtils.writeUTF8String(buf, pair.getLeft());
            ByteBufUtils.writeTag(buf, pair.getRight());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int length = buf.readShort();
        data = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            String key = ByteBufUtils.readUTF8String(buf);
            NBTTagCompound tag = ByteBufUtils.readTag(buf);
            data.add(Pair.of(key, tag));
        }
    }

}
