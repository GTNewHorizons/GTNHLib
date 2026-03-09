package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.UsernameCache;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public abstract class TeamCommandsUtils {

    static final String ARG_TEAM_NAME = "teamName";
    static final String ARG_NEW_NAME = "newName";
    static final String ARG_PLAYER = "player";

    static int success(ICommandSender sender, String transKey, Object... args) {
        ChatComponentTranslation msg = new ChatComponentTranslation(transKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(msg);
        return Command.SINGLE_SUCCESS;
    }

    static int error(ICommandSender sender, String transKey, Object... args) {
        ChatComponentTranslation msg = new ChatComponentTranslation(transKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(msg);
        return 0;
    }

    static UUID resolveTeamMemberUuid(Team team, String name) {
        EntityPlayer online = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
        if (online != null && team.isMember(online.getUniqueID())) return online.getUniqueID();

        for (UUID uuid : team.getMembers()) {
            String cachedName = UsernameCache.getLastKnownUsername(uuid);
            if (cachedName != null && cachedName.equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }

    static String formatUuidList(Set<UUID> uuids, World world) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : uuids) {
            EntityPlayer p = world.func_152378_a(uuid); // getPlayerByUUID
            if (p != null) {
                names.add(p.getCommandSenderName());
            } else {
                String cachedName = UsernameCache.getLastKnownUsername(uuid);
                names.add(cachedName == null ? uuid.toString() : cachedName);
            }
        }
        return String.join(", ", names);
    }

    static LiteralArgumentBuilder<ICommandSender> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    static <T> RequiredArgumentBuilder<ICommandSender, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    static ChatComponentText colorChatComponent(EnumChatFormatting format, String string) {
        ChatComponentText newComponent = new ChatComponentText(string);
        newComponent.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        return newComponent;
    }
}
