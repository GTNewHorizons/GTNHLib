package com.gtnewhorizon.gtnhlib.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public abstract class GTNHClientCommand extends CommandBase {

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + this.getCommandName();
    }

    protected void addChatMessage(String msg) {
        addChatMessage(new ChatComponentText(msg));
    }

    protected void addChatMessage(IChatComponent msg) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(msg);
    }

    protected void copyToClipboard(String s) {
        GuiScreen.setClipboardString(s);
    }

}
