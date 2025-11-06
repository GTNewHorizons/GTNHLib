package com.gtnewhorizon.gtnhlib.visualization;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.PacketBuffer;

import org.joml.primitives.AABBf;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketVisualizedBox implements IMessage {

    public long timeout;
    public boolean append = false, disableDepth;
    public List<VisualizedBox> boxes;

    public PacketVisualizedBox() {
    }

    public PacketVisualizedBox(List<VisualizedBox> boxes) {
        this.boxes = boxes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

        append = buffer.readBoolean();
        timeout = buffer.readLong();
        int count = buffer.readVarIntFromBuffer();

        this.boxes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int r = buffer.readByte() & 0xFF;
            int g = buffer.readByte() & 0xFF;
            int b = buffer.readByte() & 0xFF;
            int a = buffer.readByte() & 0xFF;

            float minX = buffer.readFloat();
            float minY = buffer.readFloat();
            float minZ = buffer.readFloat();
            float maxX = buffer.readFloat();
            float maxY = buffer.readFloat();
            float maxZ = buffer.readFloat();

            boxes.add(new VisualizedBox(new Color(r, g, b, a), new AABBf(minX, minY, minZ, maxX, maxY, maxZ)));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

        buffer.writeBoolean(append);
        buffer.writeLong(timeout);
        buffer.writeVarIntToBuffer(boxes.size());

        for (VisualizedBox box : boxes) {
            buffer.writeByte(box.color.getRed());
            buffer.writeByte(box.color.getGreen());
            buffer.writeByte(box.color.getBlue());
            buffer.writeByte(box.color.getAlpha());

            buffer.writeFloat(box.bounds.minX());
            buffer.writeFloat(box.bounds.minY());
            buffer.writeFloat(box.bounds.minZ());

            buffer.writeFloat(box.bounds.maxX());
            buffer.writeFloat(box.bounds.maxY());
            buffer.writeFloat(box.bounds.maxZ());
        }
    }

    public static class Handler implements IMessageHandler<PacketVisualizedBox, IMessage> {

        @Override
        public IMessage onMessage(PacketVisualizedBox message, MessageContext ctx) {
            VisualizedBoxRenderer.receiveBoxes(message.timeout, message.append, message.boxes, message.disableDepth);

            return null;
        }
    }
}
