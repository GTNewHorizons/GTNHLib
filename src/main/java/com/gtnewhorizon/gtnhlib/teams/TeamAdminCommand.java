package com.gtnewhorizon.gtnhlib.teams;

import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_NEW_NAME;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_PLAYER;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_TEAM_NAME;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_TEAM_NAME_OTHER;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.resolveTeamMemberUuid;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.argument;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.colorChatComponent;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.error;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.literal;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.success;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.teams.TeamInfoSync;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;

public class TeamAdminCommand {

    public static void register() {
        BrigadierApi.getCommandDispatcher().register(
                literal("gtnhteam_admin").requires(src -> src.canCommandSenderUseCommand(2, "gtnhteam_admin"))
                        .executes(ctx -> {
                            sendUsage(ctx.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(
                                literal("rename").then(
                                        argument(ARG_TEAM_NAME, StringArgumentType.string()).then(
                                                argument(ARG_NEW_NAME, StringArgumentType.string()).executes(
                                                        ctx -> executeAdminRename(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, ARG_TEAM_NAME),
                                                                StringArgumentType.getString(ctx, ARG_NEW_NAME))))))
                        .then(
                                literal("promote").then(
                                        argument(ARG_TEAM_NAME, StringArgumentType.string()).then(
                                                argument(ARG_PLAYER, StringArgumentType.string()).executes(
                                                        ctx -> executeAdminPromote(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, ARG_TEAM_NAME),
                                                                StringArgumentType.getString(ctx, ARG_PLAYER))))))
                        .then(
                                literal("demote").then(
                                        argument(ARG_TEAM_NAME, StringArgumentType.string()).then(
                                                argument(ARG_PLAYER, StringArgumentType.string()).executes(
                                                        ctx -> executeAdminDemote(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, ARG_TEAM_NAME),
                                                                StringArgumentType.getString(ctx, ARG_PLAYER))))))
                        .then(
                                literal("merge").then(
                                        argument(ARG_TEAM_NAME, StringArgumentType.string()).then(
                                                argument(ARG_TEAM_NAME_OTHER, StringArgumentType.string()).executes(
                                                        ctx -> executeAdminMerge(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, ARG_TEAM_NAME),
                                                                StringArgumentType
                                                                        .getString(ctx, ARG_TEAM_NAME_OTHER))))))
                        .then(
                                literal("disband").then(
                                        argument(ARG_TEAM_NAME, StringArgumentType.string()).executes(
                                                ctx -> executeAdminDisband(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_TEAM_NAME)))))
                        .then(
                                literal("info").then(
                                        argument(ARG_TEAM_NAME, StringArgumentType.string()).executes(
                                                ctx -> executeAdminInfo(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_TEAM_NAME)))))
                        .then(literal("help").executes(ctx -> executeAdminHelp(ctx.getSource()))));
    }

    private static int executeAdminRename(ICommandSender sender, String oldName, String newName) {
        Team team = TeamManager.getTeamByName(oldName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", oldName);

        if (!team.renameTeam(newName)) return error(sender, "gtnhlib.chat.teams.admin.error.name_in_use", newName);

        TeamInfoSync packet = TeamNetwork.createTeamInfoSyncPacket(team);
        TeamManager.forEachOnlineTeamMember(team, player -> NetworkHandler.instance.sendTo(packet, player));

        return success(
                sender,
                "gtnhlib.chat.teams.admin.message.renamed",
                colorChatComponent(EnumChatFormatting.GOLD, oldName),
                colorChatComponent(EnumChatFormatting.GOLD, newName));
    }

    private static int executeAdminPromote(ICommandSender sender, String teamName, String playerName) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", teamName);

        UUID uuid = resolveTeamMemberUuid(team, playerName);
        if (uuid == null || !team.isMember(uuid))
            return error(sender, "gtnhlib.chat.teams.admin.error.player_not_in_team", playerName, teamName);
        if (team.isOwner(uuid)) return error(sender, "gtnhlib.chat.teams.error.promote_owner", playerName);

        ChatComponentText playerComp = colorChatComponent(EnumChatFormatting.GOLD, playerName);
        ChatComponentText teamComp = colorChatComponent(EnumChatFormatting.GOLD, teamName);
        if (team.isOfficer(uuid)) {
            team.addOwner(uuid);
            return success(sender, "gtnhlib.chat.teams.admin.message.promoted_to_owner", playerComp, teamComp);
        } else {
            team.addOfficer(uuid);
            return success(sender, "gtnhlib.chat.teams.admin.message.promoted_to_officer", playerComp, teamComp);
        }
    }

    private static int executeAdminDemote(ICommandSender sender, String teamName, String playerName) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", teamName);

        UUID uuid = resolveTeamMemberUuid(team, playerName);
        if (uuid == null || !team.isMember(uuid))
            return error(sender, "gtnhlib.chat.teams.admin.error.player_not_in_team", playerName, teamName);
        if (!team.isOfficer(uuid)) return error(sender, "gtnhlib.chat.teams.error.demote_member", playerName);
        if (team.isOwner(uuid) && team.getOwners().size() == 1)
            return error(sender, "gtnhlib.chat.teams.admin.error.demote_last_owner", playerName, teamName);

        ChatComponentText playerComp = colorChatComponent(EnumChatFormatting.GOLD, playerName);
        ChatComponentText teamComp = colorChatComponent(EnumChatFormatting.GOLD, teamName);
        if (team.isOwner(uuid)) {
            team.removeOwner(uuid);
            return success(sender, "gtnhlib.chat.teams.admin.message.demoted_to_officer", playerComp, teamComp);
        } else {
            team.removeOfficer(uuid);
            return success(sender, "gtnhlib.chat.teams.admin.message.demoted_to_member", playerComp, teamComp);
        }
    }

    private static int executeAdminMerge(ICommandSender sender, String sourceName, String targetName) {
        Team sourceTeam = TeamManager.getTeamByName(sourceName);
        if (sourceTeam == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", sourceName);
        Team targetTeam = TeamManager.getTeamByName(targetName);
        if (targetTeam == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", targetName);

        if (sourceTeam.getTeamId().equals(targetTeam.getTeamId())) {
            return error(sender, "gtnhlib.chat.teams.admin.message.merge_teams_same");
        }

        List<UUID> allMembers = new ArrayList<>(sourceTeam.getMembers());
        allMembers.addAll(targetTeam.getMembers());

        TeamManager.mergeTeams(targetTeam, sourceTeam);

        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.merge_complete",
                colorChatComponent(EnumChatFormatting.GOLD, sourceName),
                colorChatComponent(EnumChatFormatting.GOLD, targetName));
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);

        for (UUID memberUuid : allMembers) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
            if (member != null) member.addChatMessage(notification);
        }

        return success(
                sender,
                "gtnhlib.chat.teams.admin.message.merged",
                colorChatComponent(EnumChatFormatting.GOLD, sourceName),
                colorChatComponent(EnumChatFormatting.GOLD, targetName));
    }

    private static int executeAdminDisband(ICommandSender sender, String teamName) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", teamName);

        List<UUID> members = new ArrayList<>(team.getMembers());

        TeamManager.TEAMS.remove(team);
        TeamManager.TEAM_MAP.remove(team.getTeamId());
        TeamManager.PENDING_MERGE_REQUESTS.remove(team);
        TeamManager.PENDING_MERGE_REQUESTS.values().forEach(teamSet -> teamSet.remove(team));
        team.markRemoved();
        for (Set<Team> teams : TeamManager.PENDING_INVITES.values()) {
            teams.remove(team);
        }

        ChatComponentTranslation notice = new ChatComponentTranslation(
                "gtnhlib.chat.teams.admin.message.team_disbanded",
                colorChatComponent(EnumChatFormatting.GOLD, teamName));
        notice.getChatStyle().setColor(EnumChatFormatting.RED);

        for (UUID uuid : members) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), uuid);
            String name = member != null ? member.getCommandSenderName() : uuid.toString();
            Team newTeam = TeamManager.getOrCreateTeam(name, uuid);
            TeamManager.copyTeamData(team, newTeam, uuid, TeamDataCopyReason.JoinedNewTeam);
            if (member != null) member.addChatMessage(notice);
        }

        return success(
                sender,
                "gtnhlib.chat.teams.admin.message.disbanded",
                colorChatComponent(EnumChatFormatting.GOLD, teamName));
    }

    private static int executeAdminInfo(ICommandSender sender, String teamName) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", teamName);

        TeamCommandsUtils.printTeamInfo(sender, team);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAdminHelp(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.1"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.2"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.3"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.4"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.5"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.6"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.7"));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendUsage(ICommandSender sender) {
        ChatComponentTranslation msg = new ChatComponentTranslation("gtnhlib.chat.teams.admin.message.usage");
        msg.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(msg);
    }
}
