package com.gtnewhorizon.gtnhlib.network;

import java.io.IOException;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizon.gtnhlib.client.title.TitleAPI;
import com.gtnewhorizon.gtnhlib.network.base.IPacket;
import com.gtnewhorizon.gtnhlib.network.base.NetworkUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageTitle implements IPacket {

    /** 0=TITLE, 1=SUBTITLE, 2=TIMES, 3=CLEAR, 4=RESET, 5=ICON */
    public int action;

    /** For TITLE/SUBTITLE: the IChatComponent JSON */
    public String componentJson;

    /** For TIMES: fadeIn, stay, fadeOut ticks */
    public int fadeIn, stay, fadeOut;

    /** For ICON: the item stack (null = clear icon) */
    public ItemStack iconStack;

    public MessageTitle() {}

    public static MessageTitle title(IChatComponent component) {
        MessageTitle msg = new MessageTitle();
        msg.action = 0;
        msg.componentJson = IChatComponent.Serializer.func_150696_a(component);
        return msg;
    }

    public static MessageTitle subtitle(IChatComponent component) {
        MessageTitle msg = new MessageTitle();
        msg.action = 1;
        msg.componentJson = IChatComponent.Serializer.func_150696_a(component);
        return msg;
    }

    public static MessageTitle times(int fadeIn, int stay, int fadeOut) {
        MessageTitle msg = new MessageTitle();
        msg.action = 2;
        msg.fadeIn = fadeIn;
        msg.stay = stay;
        msg.fadeOut = fadeOut;
        return msg;
    }

    public static MessageTitle clear() {
        MessageTitle msg = new MessageTitle();
        msg.action = 3;
        return msg;
    }

    public static MessageTitle reset() {
        MessageTitle msg = new MessageTitle();
        msg.action = 4;
        return msg;
    }

    public static MessageTitle icon(ItemStack stack) {
        MessageTitle msg = new MessageTitle();
        msg.action = 5;
        msg.iconStack = stack;
        return msg;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeByte(action);
        switch (action) {
            case 0:
            case 1:
                buf.writeStringToBuffer(componentJson);
                break;
            case 2:
                buf.writeInt(fadeIn);
                buf.writeInt(stay);
                buf.writeInt(fadeOut);
                break;
            case 5:
                buf.writeBoolean(iconStack != null);
                if (iconStack != null) {
                    buf.writeItemStackToBuffer(iconStack);
                }
                break;
        }
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        action = buf.readByte();
        switch (action) {
            case 0:
            case 1:
                componentJson = buf.readStringFromBuffer(NetworkUtils.MAX_STRING_BYTES);
                break;
            case 2:
                fadeIn = buf.readInt();
                stay = buf.readInt();
                fadeOut = buf.readInt();
                break;
            case 5:
                if (buf.readBoolean()) {
                    iconStack = buf.readItemStackFromBuffer();
                } else {
                    iconStack = null;
                }
                break;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IPacket executeClient(NetHandlerPlayClient handler) {
        switch (action) {
            case 0: // TITLE
                TitleAPI.setTitle(deserialize(componentJson));
                break;
            case 1: // SUBTITLE
                TitleAPI.setSubtitle(deserialize(componentJson));
                break;
            case 2: // TIMES
                TitleAPI.setTimes(fadeIn, stay, fadeOut);
                break;
            case 3: // CLEAR
                TitleAPI.clear();
                break;
            case 4: // RESET
                TitleAPI.clear();
                TitleAPI.reset();
                break;
            case 5: // ICON
                TitleAPI.setIcon(iconStack);
                break;
        }
        return null;
    }

    private static IChatComponent deserialize(String json) {
        if (json == null || json.isEmpty()) return new ChatComponentText("");
        try {
            return IChatComponent.Serializer.func_150699_a(json);
        } catch (Exception e) {
            return new ChatComponentText(json);
        }
    }

}
