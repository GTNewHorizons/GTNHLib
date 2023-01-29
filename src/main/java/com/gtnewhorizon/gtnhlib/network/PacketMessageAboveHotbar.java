package com.gtnewhorizon.gtnhlib.network;

import net.minecraft.util.IChatComponent;

import com.gtnewhorizon.gtnhlib.GTNHLib;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketMessageAboveHotbar implements IMessage {

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
    public void fromBytes(ByteBuf buf) {
        this.chatComponent = IChatComponent.Serializer.func_150699_a(ByteBufUtils.readUTF8String(buf));
        this.displayDuration = buf.readInt();
        this.drawShadow = buf.readBoolean();
        this.shouldFade = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, IChatComponent.Serializer.func_150696_a(this.chatComponent));
        buf.writeInt(this.displayDuration);
        buf.writeBoolean(this.drawShadow);
        buf.writeBoolean(this.shouldFade);
    }

    public static class HandlerMessageAboveHotbar implements IMessageHandler<PacketMessageAboveHotbar, IMessage> {

        @Override
        public IMessage onMessage(PacketMessageAboveHotbar message, MessageContext ctx) {
            GTNHLib.proxy.printMessageAboveHotbar(
                    message.chatComponent.getFormattedText(),
                    message.displayDuration,
                    message.drawShadow,
                    message.shouldFade);
            return null;
        }
    }
}
