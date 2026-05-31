package com.gtnewhorizon.gtnhlib.teams;

import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_NEW_NAME;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_PLAYER;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_TEAM_NAME;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_TEAM_NAME_OTHER;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.resolveTeamMemberUuid;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.argument;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.error;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.literal;

import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
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
                                literal("kick").then(
                                        argument(ARG_TEAM_NAME, StringArgumentType.string()).then(
                                                argument(ARG_PLAYER, StringArgumentType.string()).executes(
                                                        ctx -> executeAdminKick(
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
        if (newName.length() > Team.MAX_TEAM_NAME_LENGTH) {
            return error(sender, "gtnhlib.chat.teams.message.team_name_too_long");
        }
        Team team = TeamManager.getTeamByName(oldName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", oldName);

        if (!team.renameTeam(newName)) return error(sender, "gtnhlib.chat.teams.admin.error.name_in_use", newName);

        TeamActions.onRename(team, oldName, newName, true, sender);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAdminPromote(ICommandSender sender, String teamName, String playerName) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", teamName);

        UUID uuid = resolveTeamMemberUuid(team, playerName);
        if (uuid == null || !team.isMember(uuid))
            return error(sender, "gtnhlib.chat.teams.admin.error.player_not_in_team", playerName, teamName);
        if (team.isOwner(uuid)) return error(sender, "gtnhlib.chat.teams.error.promote_owner", playerName);

        TeamActions.onPromote(team, uuid, true, sender);

        return Command.SINGLE_SUCCESS;
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

        TeamActions.onDemote(team, uuid, true, sender);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAdminKick(ICommandSender sender, String teamName, String playerName) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", teamName);

        UUID uuid = resolveTeamMemberUuid(team, playerName);
        if (uuid == null || !team.isMember(uuid))
            return error(sender, "gtnhlib.chat.teams.admin.error.player_not_in_team", playerName, teamName);
        if (team.isOwner(uuid) && team.getOwners().size() == 1)
            return error(sender, "gtnhlib.chat.teams.admin.error.kick_last_owner", playerName, teamName);

        TeamActions.onDemote(team, uuid, true, sender);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAdminMerge(ICommandSender sender, String sourceName, String targetName) {
        Team sourceTeam = TeamManager.getTeamByName(sourceName);
        if (sourceTeam == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", sourceName);
        Team targetTeam = TeamManager.getTeamByName(targetName);
        if (targetTeam == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", targetName);

        if (sourceTeam.getTeamId().equals(targetTeam.getTeamId())) {
            return error(sender, "gtnhlib.chat.teams.admin.message.merge_teams_same");
        }

        TeamActions.onMergeAccept(sourceTeam, targetTeam, true, sender);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAdminDisband(ICommandSender sender, String teamName) {
        Team team = TeamManager.getTeamByName(teamName);
        if (team == null) return error(sender, "gtnhlib.chat.teams.admin.error.team_not_found", teamName);
        if (!team.canBeDisbanded()) return error(sender, "gtnhlib.chat.teams.admin.error.cannot_disband_solo_team");

        TeamActions.onDisband(team, true, sender);

        return Command.SINGLE_SUCCESS;
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
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.admin.help.8"));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendUsage(ICommandSender sender) {
        ChatComponentTranslation msg = new ChatComponentTranslation("gtnhlib.chat.teams.admin.message.usage");
        msg.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(msg);
    }
}
