package com.gtnewhorizon.gtnhlib.brigadier;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.tree.CommandNode;

public class BrigadierCommandWrapper extends CommandBase {

    private final String name;

    public BrigadierCommandWrapper(String name) {
        this.name = name;
    }

    @Override
    public String getCommandName() {
        return this.name;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + this.name;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        CommandNode<ICommandSender> node = BrigadierApi.getCommandDispatcher().getRoot().getChild(this.name);
        return node == null || node.canUse(sender);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        StringBuilder command = new StringBuilder(this.name);
        for (String arg : args) {
            command.append(" ").append(arg);
        }
        BrigadierApi.executeCommand(sender, command.toString());
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        StringBuilder command = new StringBuilder(this.name);
        for (String arg : args) {
            command.append(" ").append(arg);
        }
        return BrigadierApi.getPossibleCommands(sender, command.toString());
    }
}
