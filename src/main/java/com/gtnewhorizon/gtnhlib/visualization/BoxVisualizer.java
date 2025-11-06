package com.gtnewhorizon.gtnhlib.visualization;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.gtnewhorizon.gtnhlib.network.NetworkHandler;

public class BoxVisualizer {

    private static final int MAX_PACKET_SIZE = 3200,
        BYTES_PER_BOX = 4 + 4 * 3 + 4 * 3,
        MAX_BOXES_PER_PACKET = MAX_PACKET_SIZE / BYTES_PER_BOX;

    public static void sendBoxes(EntityPlayerMP player, Duration timeout, Collection<VisualizedBox> boxes, boolean disableDepth) {
        List<VisualizedBox> boxList = new ArrayList<>(boxes);

        for (int i = 0; i < boxList.size(); i += MAX_BOXES_PER_PACKET) {
            int toSend = Math.min(boxList.size() - i, MAX_BOXES_PER_PACKET);

            PacketVisualizedBox packet = new PacketVisualizedBox(boxList.subList(i, i + toSend));

            packet.append = i > 0;
            packet.timeout = timeout.toMillis();
            packet.disableDepth = disableDepth;

            NetworkHandler.instance.sendTo(packet, player);
        }
    }
}
