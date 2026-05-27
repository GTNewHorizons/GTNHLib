package com.gtnewhorizon.gtnhlib.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public final class CommandUtils {

    public static int success(ICommandSender sender, String transKey, Object... args) {
        ChatComponentTranslation msg = new ChatComponentTranslation(transKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(msg);
        return Command.SINGLE_SUCCESS;
    }

    public static int error(ICommandSender sender, String transKey, Object... args) {
        ChatComponentTranslation msg = new ChatComponentTranslation(transKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(msg);
        return 0;
    }

    public static LiteralArgumentBuilder<ICommandSender> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<ICommandSender, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static ChatComponentText colorChatComponent(EnumChatFormatting format, String string) {
        ChatComponentText newComponent = new ChatComponentText(string);
        newComponent.getChatStyle().setColor(format);
        return newComponent;
    }
}
