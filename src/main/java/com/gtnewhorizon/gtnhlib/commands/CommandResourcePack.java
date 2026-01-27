package com.gtnewhorizon.gtnhlib.commands;

import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater.ResourcePackUpdateChecker;

public class CommandResourcePack extends GTNHClientCommand {

    @Override
    public String getCommandName() {
        return "resourcepack";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/resourcepack updateCheck [force]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            printHelp();
            return;
        }
        if ("updateCheck".equalsIgnoreCase(args[0])) {
            boolean force = args.length > 1 && "force".equalsIgnoreCase(args[1]);
            if (!GTNHLibConfig.enableResourcePackUpdateCheck && !force) {
                addChatMessage(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.disabled"));
                return;
            }
            ResourcePackUpdateChecker.runManualCheck(force);
            return;
        }
        printHelp();
    }

    private void printHelp() {
        addChatMessage(StatCollector.translateToLocal("gtnhlib.chat.rpupdater.commands"));
        addChatMessage("/resourcepack updateCheck [force]");
        addChatMessage("/resourcepack help");
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "updateCheck", "help");
        }
        if (args.length == 2 && "updateCheck".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "force");
        }
        return null;
    }
}
