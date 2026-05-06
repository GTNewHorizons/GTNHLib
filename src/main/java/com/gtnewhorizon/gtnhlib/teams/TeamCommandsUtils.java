package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.HashSet;
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

import com.gtnewhorizon.gtnhlib.util.CommandUtils;

public abstract class TeamCommandsUtils {

    static final String ARG_TEAM_NAME = "teamName";
    static final String ARG_NEW_NAME = "newName";
    static final String ARG_PLAYER = "player";

    static UUID resolveTeamMemberUuid(Team team, String name) {
        EntityPlayer online = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
        if (online != null && team.isMember(online.getUniqueID())) return online.getUniqueID();

        for (UUID uuid : team.getMembers()) {
            String cachedName = UsernameCache.getLastKnownUsername(uuid);
            if (cachedName != null && cachedName.equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }

    static String formatUuidList(Set<UUID> uuids, World world, Set<UUID> seen) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : uuids) {
            if (seen.add(uuid)) {
                EntityPlayer p = world.func_152378_a(uuid); // getPlayerByUUID
                if (p != null) {
                    names.add(p.getCommandSenderName());
                } else {
                    String cachedName = UsernameCache.getLastKnownUsername(uuid);
                    names.add(cachedName == null ? uuid.toString() : cachedName);
                }
            }
        }
        return String.join(", ", names);
    }

    static void printTeamInfo(ICommandSender sender, Team team) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "=== " + team.getTeamName() + " ==="));

        Set<UUID> seen = new HashSet<>();
        ChatComponentTranslation ownersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.owners",
                CommandUtils.colorChatComponent(
                        EnumChatFormatting.WHITE,
                        formatUuidList(team.getOwners(), sender.getEntityWorld(), seen)));
        ownersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(ownersTrans);

        ChatComponentTranslation officersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.officers",
                CommandUtils.colorChatComponent(
                        EnumChatFormatting.WHITE,
                        formatUuidList(team.getOfficers(), sender.getEntityWorld(), seen)));
        officersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(officersTrans);

        ChatComponentTranslation membersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.members",
                CommandUtils.colorChatComponent(
                        EnumChatFormatting.WHITE,
                        formatUuidList(team.getMembers(), sender.getEntityWorld(), seen)));
        membersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(membersTrans);
    }

}
