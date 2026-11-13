package com.gtnewhorizon.gtnhlib.test.block;

import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;

import com.gtnewhorizon.gtnhlib.commands.GTNHClientCommand;

public class CommandBlackHole extends GTNHClientCommand {

    private static final String[] SETTINGS = { "radius", "horizon", "fadeStart", "fadeEnd", "captureSize",
            "captureRate" };

    private final RenderBlackHole renderer;

    public CommandBlackHole(RenderBlackHole renderer) {
        this.renderer = renderer;
    }

    @Override
    public String getCommandName() {
        return "blackhole";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/blackhole profile [seconds] | capture | settings | set <setting> <value>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length >= 1 && args[0].equals("profile")) {
            final int seconds = args.length >= 2 ? parseIntBounded(sender, args[1], 1, 300) : 10;
            renderer.startProfile(seconds);
            addChatMessage("Profiling the black hole for " + seconds + " s.");
        } else if (args.length == 1 && args[0].equals("capture")) {
            addChatMessage("World capture " + (renderer.capture.toggle() ? "on." : "off."));
        } else if (args.length == 1 && args[0].equals("settings")) {
            addChatMessage("radius " + BlackHoleSettings.radius + " blocks");
            addChatMessage("horizon " + BlackHoleSettings.horizon + " of the radius");
            addChatMessage(
                    "fadeStart " + BlackHoleSettings.fadeStart
                            + ", fadeEnd "
                            + BlackHoleSettings.fadeEnd
                            + " of the radius: where bending fades out");
            addChatMessage("captureSize " + BlackHoleSettings.captureSize + " texels per face edge");
            addChatMessage("captureRate " + BlackHoleSettings.captureRate + " faces per second");
        } else if (args.length == 3 && args[0].equals("set")) {
            set(sender, args[1], args[2]);
            BlackHoleSettings.version++;
            addChatMessage(args[1] + " set to " + args[2] + ".");
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    private void set(ICommandSender sender, String setting, String value) {
        switch (setting) {
            case "radius" -> BlackHoleSettings.radius = parseDoubleBounded(sender, value, 1, 64);
            case "horizon" -> BlackHoleSettings.horizon = (float) parseDoubleBounded(sender, value, 0.005, 0.5);
            case "fadeStart" -> BlackHoleSettings.fadeStart = (float) parseDoubleBounded(
                    sender,
                    value,
                    0,
                    BlackHoleSettings.fadeEnd - 0.01);
            case "fadeEnd" -> BlackHoleSettings.fadeEnd = (float) parseDoubleBounded(
                    sender,
                    value,
                    BlackHoleSettings.fadeStart + 0.01,
                    1);
            case "captureSize" -> BlackHoleSettings.captureSize = parseIntBounded(sender, value, 16, 2048);
            case "captureRate" -> BlackHoleSettings.captureRate = parseDoubleBounded(sender, value, 0.1, 240);
            default -> throw new WrongUsageException("Settings: " + String.join(", ", SETTINGS));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "profile", "capture", "settings", "set");
        if (args.length == 2 && args[0].equals("set")) return getListOfStringsMatchingLastWord(args, SETTINGS);
        return null;
    }
}
