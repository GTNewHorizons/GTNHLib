package com.gtnewhorizon.gtnhlib.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;

import net.minecraftforge.common.util.ForgeDirection;

import org.joml.Vector3d;
import org.joml.Vector3i;

import lombok.Setter;

@SuppressWarnings("unused")
public final class CommandArgumentParser {

    private final ICommand command;
    private int index;
    private final String[] args;
    private final ICommandSender sender;
    @Setter
    private EntityPlayerMP player;

    public CommandArgumentParser(ICommand command, String[] args, ICommandSender sender) {
        this.command = command;
        this.args = args;
        this.sender = sender;
        this.player = sender instanceof EntityPlayerMP p ? p : null;
    }

    public boolean hasNext() {
        return index < args.length;
    }

    public String nextString() {
        if (!hasNext()) {
            throw new WrongUsageException(command.getCommandUsage(sender));
        }

        return args[index++];
    }

    public int nextInteger() {
        return CommandBase.parseInt(sender, nextString());
    }

    public int nextInteger(int min) {
        return CommandBase.parseIntWithMin(sender, nextString(), min);
    }

    public int nextInteger(int min, int max) {
        return CommandBase.parseIntBounded(sender, nextString(), min, max);
    }

    public double nextDouble() {
        return CommandBase.parseDouble(sender, nextString());
    }

    public double nextDouble(double min) {
        return CommandBase.parseDoubleWithMin(sender, nextString(), min);
    }

    public double nextDouble(double min, double max) {
        return CommandBase.parseDoubleBounded(sender, nextString(), min, max);
    }

    public boolean nextBoolean() {
        return CommandBase.parseBoolean(sender, nextString());
    }

    public double nextX() {
        return player == null
            ? nextDouble()
            : CommandBase.func_110665_a(sender, player.posX, nextString(), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public double nextY() {
        return player == null
            ? nextDouble()
            : CommandBase.func_110665_a(sender, player.posY, nextString(), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public double nextZ() {
        return player == null
            ? nextDouble()
            : CommandBase.func_110665_a(sender, player.posZ, nextString(), Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public Vector3d nextPos() {
        return new Vector3d(nextX(), nextY(), nextZ());
    }

    public Vector3i nextBlockPos() {
        return new Vector3i(
            MathHelper.floor_double(nextX()),
            MathHelper.floor_double(nextY()),
            MathHelper.floor_double(nextZ()));
    }

    public EntityPlayerMP nextPlayer() {
        return CommandBase.getPlayer(sender, nextString());
    }

    public ForgeDirection nextDirection() {
        String text = nextString();

        try {
            return ForgeDirection.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WrongUsageException("Illegal direction: " + text);
        }
    }

    public EntityPlayerMP getPlayerIfNeeded() {
        if (this.player == null) {
            this.player = nextPlayer();
        }

        return this.player;
    }
}
