package com.gtnewhorizon.gtnhlib.commands;

import net.minecraft.command.ICommandSender;

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
                addChatMessage("Resource pack update checking is disabled in config.");
                return;
            }
            ResourcePackUpdateChecker.CheckResult result = ResourcePackUpdateChecker.runManualCheck(force);
            if (result.cooldownBlocked) {
                addChatMessage(
                        "Update check is on cooldown due to a recent failure. Try later or use /resourcepack updateCheck force.");
                return;
            }
            if (result.updatesFound == 0) {
                addChatMessage("No updates found for active resource packs.");
            }
            return;
        }
        printHelp();
    }

    private void printHelp() {
        addChatMessage("Resource pack commands:");
        addChatMessage("/resourcepack updateCheck [force]");
        addChatMessage("/resourcepack help");
    }
}
