package com.gtnewhorizon.gtnhlib.network;

import java.io.IOException;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketMessageAboveHotbar implements IPacket {

    private IChatComponent chatComponent;
    private int displayDuration;
    private boolean drawShadow;
    private boolean shouldFade;

    @SuppressWarnings("unused")
    public PacketMessageAboveHotbar() {}

    public PacketMessageAboveHotbar(IChatComponent chatComponent, int displayDuration, boolean drawShadow,
            boolean shouldFade) {
        this.chatComponent = chatComponent;
        this.displayDuration = displayDuration;
        this.drawShadow = drawShadow;
        this.shouldFade = shouldFade;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeStringToBuffer(IChatComponent.Serializer.func_150696_a(chatComponent));
        buf.writeInt(displayDuration);
        buf.writeBoolean(drawShadow);
        buf.writeBoolean(shouldFade);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        chatComponent = IChatComponent.Serializer
                .func_150699_a(buf.readStringFromBuffer(NetworkUtils.MAX_STRING_BYTES));
        displayDuration = buf.readInt();
        drawShadow = buf.readBoolean();
        shouldFade = buf.readBoolean();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        GTNHLib.proxy
                .printMessageAboveHotbar(chatComponent.getFormattedText(), displayDuration, drawShadow, shouldFade);
        return null;
    }
}
