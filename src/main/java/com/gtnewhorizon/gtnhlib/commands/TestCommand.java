package com.gtnewhorizon.gtnhlib.commands;

import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentEnergy;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentFluid;
import com.gtnewhorizon.gtnhlib.chat.customcomponents.ChatComponentNumber;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.math.BigInteger;

public class TestCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "test";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "test";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(new ChatComponentNumber(1_000_000));
        sender.addChatMessage(new ChatComponentEnergy(1_000_000));
        sender.addChatMessage(new ChatComponentFluid(1_000_000));

        sender.addChatMessage(new ChatComponentNumber(1_000_000d));
        sender.addChatMessage(new ChatComponentEnergy(1_000_000d));
        sender.addChatMessage(new ChatComponentFluid(1_000_000d));

        sender.addChatMessage(new ChatComponentNumber(1_000_000L));
        sender.addChatMessage(new ChatComponentEnergy(1_000_000L));
        sender.addChatMessage(new ChatComponentFluid(1_000_000L));

        sender.addChatMessage(new ChatComponentNumber(1_000_000f));
        sender.addChatMessage(new ChatComponentEnergy(1_000_000f));
        sender.addChatMessage(new ChatComponentFluid(1_000_000f));

        sender.addChatMessage(new ChatComponentNumber(BigInteger.valueOf(1_000_000)));
        sender.addChatMessage(new ChatComponentEnergy(BigInteger.valueOf(1_000_000)));
        sender.addChatMessage(new ChatComponentFluid(BigInteger.valueOf(1_000_000)));
    }
}
