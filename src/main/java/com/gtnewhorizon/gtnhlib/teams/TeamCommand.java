package com.gtnewhorizon.gtnhlib.teams;

import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_NEW_NAME;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_PLAYER;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_TEAM_NAME;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.resolveTeamMemberUuid;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.argument;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.error;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.literal;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.UsernameCache;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.GTNHLibConfig;
import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.gtnewhorizon.gtnhlib.integration.mui2.TeamGuiFactory;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public class TeamCommand {

    public static void register() {
        BrigadierApi.getCommandDispatcher().register(literal(GTNHLibConfig.teamCommandRoot).executes(ctx -> {
            sendUsage(ctx.getSource());
            return Command.SINGLE_SUCCESS;
        }).then(literal("gui").executes(ctx -> executeGui(ctx.getSource())))

                .then(
                        literal("rename").then(
                                argument(ARG_NEW_NAME, StringArgumentType.string()).executes(
                                        ctx -> executeRename(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, ARG_NEW_NAME)))))

                .then(
                        literal("invite")
                                .then(argument(ARG_PLAYER, StringArgumentType.word()).suggests((ctx, builder) -> {
                                    for (Object p : ctx.getSource().getEntityWorld().playerEntities) {
                                        if (p instanceof EntityPlayer ep) builder.suggest(ep.getCommandSenderName());
                                    }
                                    return builder.buildFuture();
                                }).executes(
                                        ctx -> executeInvite(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, ARG_PLAYER)))))

                .then(
                        literal("accept").executes(ctx -> executeAccept(ctx.getSource(), "")).then(
                                argument(ARG_TEAM_NAME, StringArgumentType.string())
                                        .suggests((ctx, builder) -> suggestPendingInvites(ctx.getSource(), builder))
                                        .executes(
                                                ctx -> executeAccept(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_TEAM_NAME)))))

                .then(
                        literal("deny").executes(ctx -> executeDeny(ctx.getSource(), "")).then(
                                argument(ARG_TEAM_NAME, StringArgumentType.string())
                                        .suggests((ctx, builder) -> suggestPendingInvites(ctx.getSource(), builder))
                                        .executes(
                                                ctx -> executeDeny(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_TEAM_NAME)))))

                .then(literal("leave").executes(ctx -> executeLeave(ctx.getSource())))

                .then(
                        literal("promote").then(
                                argument(ARG_PLAYER, StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestTeamMembers(ctx.getSource(), builder))
                                        .executes(
                                                ctx -> executePromote(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_PLAYER)))))

                .then(
                        literal("demote").then(
                                argument(ARG_PLAYER, StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestTeamMembers(ctx.getSource(), builder))
                                        .executes(
                                                ctx -> executeDemote(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_PLAYER)))))
                .then(
                        literal("kick").then(
                                argument(ARG_PLAYER, StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestTeamMembers(ctx.getSource(), builder))
                                        .executes(
                                                ctx -> executeKick(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_PLAYER)))))

                .then(literal("info").executes(ctx -> executeInfo(ctx.getSource())))

                .then(
                        literal("merge")
                                .then(
                                        literal("request").then(
                                                argument(ARG_TEAM_NAME, StringArgumentType.string()).suggests(
                                                        (ctx, builder) -> suggestOtherTeams(ctx.getSource(), builder))
                                                        .executes(
                                                                ctx -> executeMergeRequest(
                                                                        ctx.getSource(),
                                                                        StringArgumentType
                                                                                .getString(ctx, ARG_TEAM_NAME)))))
                                .then(
                                        literal("cancel").then(
                                                argument(ARG_TEAM_NAME, StringArgumentType.string()).suggests(
                                                        (ctx, builder) -> suggestOtherTeams(ctx.getSource(), builder))
                                                        .executes(
                                                                ctx -> executeMergeCancel(
                                                                        ctx.getSource(),
                                                                        StringArgumentType
                                                                                .getString(ctx, ARG_TEAM_NAME)))))
                                .then(
                                        literal("accept").executes(ctx -> executeMergeAccept(ctx.getSource(), ""))
                                                .then(
                                                        argument(ARG_TEAM_NAME, StringArgumentType.string())
                                                                .suggests(
                                                                        (ctx, builder) -> suggestPendingMerges(
                                                                                ctx.getSource(),
                                                                                builder))
                                                                .executes(
                                                                        ctx -> executeMergeAccept(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        ctx,
                                                                                        ARG_TEAM_NAME)))))
                                .then(
                                        literal("deny").executes(ctx -> executeMergeDeny(ctx.getSource(), ""))
                                                .then(
                                                        argument(ARG_TEAM_NAME, StringArgumentType.string())
                                                                .suggests(
                                                                        (ctx, builder) -> suggestPendingMerges(
                                                                                ctx.getSource(),
                                                                                builder))
                                                                .executes(
                                                                        ctx -> executeMergeDeny(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        ctx,
                                                                                        ARG_TEAM_NAME))))))
                .then(literal("disband").executes(ctx -> executeDisband(ctx.getSource())))

                .then(literal("help").executes(ctx -> executeHelp(ctx.getSource()))));
    }

    private static int executeGui(ICommandSender sender) {
        if (!GTNHLib.isMui2Loaded) {
            return error(sender, "gtnhlib.chat.teams.error.mui2_not_loaded");
        }

        EntityPlayerMP player = TeamCommandsUtils.asPlayerMP(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        TeamGuiFactory.INSTANCE.open(player, team.getTeamId());

        return Command.SINGLE_SUCCESS;
    }

    private static int executeRename(ICommandSender sender, String newName) {
        if (newName.length() > Team.MAX_TEAM_NAME_LENGTH) {
            return error(sender, "gtnhlib.chat.teams.message.team_name_too_long");
        }
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_rename");

        String oldName = team.getTeamName();
        if (!team.renameTeam(newName)) {
            return error(sender, "gtnhlib.chat.teams.error.name_in_use");
        }

        TeamActions.onRename(team, oldName, newName, false, null);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeInvite(ICommandSender sender, String targetName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isOfficer(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_officer_invite");

        EntityPlayer target = player.worldObj.getPlayerEntityByName(targetName);
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.not_online", targetName);
        if (team.isMember(target.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.invite_teammate", targetName);
        if (target.getUniqueID().equals(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.invite_self");

        TeamActions.onInvite(team, player, target);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAccept(ICommandSender sender, String teamName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;
        UUID playerId = player.getUniqueID();

        Set<Team> invites = TeamManager.getPendingInvites(playerId);
        if (invites == null || invites.isEmpty()) return error(sender, "gtnhlib.chat.teams.error.no_invite");

        Team invitingTeam;
        if (invites.size() == 1) {
            invitingTeam = invites.iterator().next();
        } else if (teamName.isEmpty()) {
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_invite", TeamCommandsUtils.getCommandRoot());
        } else {
            invitingTeam = TeamManager.getTeamByName(teamName);
            if (invitingTeam == null || !invites.contains(invitingTeam))
                return error(sender, "gtnhlib.chat.teams.error.no_invite_specific", teamName);
        }

        Team currentTeam = TeamManager.getTeamByPlayer(playerId);
        assert currentTeam != null;
        // Don't allow joining if player is sole owner of their team AND there are other members
        if (currentTeam.isOwner(playerId) && currentTeam.getOwners().size() == 1
                && currentTeam.getMembers().size() > 1) {
            return error(sender, "gtnhlib.chat.teams.error.last_owner_leave");
        }

        TeamActions.onAccept(invitingTeam, player);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDeny(ICommandSender sender, String teamName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Set<Team> invites = TeamManager.getPendingInvites(player.getUniqueID());
        if (invites == null || invites.isEmpty()) return error(sender, "gtnhlib.chat.teams.error.no_invite");

        Team specificTeam;
        if (invites.size() == 1) {
            specificTeam = invites.iterator().next();
        } else if (teamName.isEmpty()) {
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_invite", TeamCommandsUtils.getCommandRoot());
        } else {
            specificTeam = TeamManager.getTeamByName(teamName);
            if (specificTeam == null || !invites.contains(specificTeam))
                return error(sender, "gtnhlib.chat.teams.error.no_invite_specific", teamName);
        }

        TeamActions.onDeny(specificTeam, player);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeLeave(ICommandSender sender) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;
        UUID playerId = player.getUniqueID();

        Team team = TeamManager.getTeamByPlayer(playerId);
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        if (team.getMembers().size() == 1) {
            return error(sender, "gtnhlib.chat.teams.error.last_member_leave");
        }
        if (team.isOwner(playerId) && team.getOwners().size() == 1) {
            return error(sender, "gtnhlib.chat.teams.error.last_owner_leave");
        }

        TeamActions.onLeave(player);

        return Command.SINGLE_SUCCESS;
    }

    private static int executePromote(ICommandSender sender, String targetName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_promote");

        UUID targetUuid = resolveTeamMemberUuid(team, targetName);
        if (targetUuid == null) return error(sender, "gtnhlib.chat.teams.error.other_not_in_team", targetName);
        if (team.isOwner(targetUuid)) return error(sender, "gtnhlib.chat.teams.error.promote_owner", targetName);

        TeamActions.onPromote(team, targetUuid, false, null);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDemote(ICommandSender sender, String targetName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_demote");

        UUID targetUuid = resolveTeamMemberUuid(team, targetName);
        if (targetUuid == null) return error(sender, "gtnhlib.chat.teams.error.other_not_in_team", targetName);
        if (targetUuid.equals(player.getUniqueID()) && team.getOwners().size() == 1)
            return error(sender, "gtnhlib.chat.teams.error.last_owner_demote");
        if (!team.isOfficer(targetUuid)) return error(sender, "gtnhlib.chat.teams.error.demote_member", targetName);

        TeamActions.onDemote(team, targetUuid, false, null);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeKick(ICommandSender sender, String targetName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;
        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        UUID targetUuid = resolveTeamMemberUuid(team, targetName);
        if (targetUuid == null) return error(sender, "gtnhlib.chat.teams.error.other_not_in_team", targetName);
        if (targetUuid.equals(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.cannot_kick_self");
        if (!TeamCommandsUtils.canKick(team.getRole(player.getUniqueID()), team.getRole(targetUuid)))
            return error(sender, "gtnhlib.chat.teams.error.kick_not_allowed");
        TeamActions.onKick(team, targetUuid, false, null);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeInfo(ICommandSender sender) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        TeamCommandsUtils.printTeamInfo(sender, team);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMergeRequest(ICommandSender sender, String targetTeamName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team source = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (source == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!source.isOwner(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.not_owner_merge_request");

        Team target = TeamManager.getTeamByName(targetTeamName);
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.team_not_found", targetTeamName);
        if (target == source) return error(sender, "gtnhlib.chat.teams.error.merge_self");

        if (TeamManager.hasPendingMergeRequest(source, target))
            return error(sender, "gtnhlib.chat.teams.error.merge_already_requested", targetTeamName);

        TeamActions.onMergeRequest(player, source, target);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMergeCancel(ICommandSender sender, String targetTeamName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team source = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (source == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!source.isOwner(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.not_owner_merge_request");
        Team target = TeamManager.getTeamByName(targetTeamName);
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.team_not_found", targetTeamName);
        if (!TeamManager.hasPendingMergeRequest(source, target))
            return error(sender, "gtnhlib.chat.teams.error.no_such_merge_request", targetTeamName);

        TeamActions.onMergeCancel(player, source, target);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMergeAccept(ICommandSender sender, String sourceTeamName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team target = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!target.isOwner(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.not_owner_merge_response");

        Set<Team> pendingMerges = TeamManager.getPendingMergeRequests(target);
        if (pendingMerges == null || pendingMerges.isEmpty())
            return error(sender, "gtnhlib.chat.teams.error.no_merge_request");

        Team source;
        if (pendingMerges.size() == 1) {
            source = pendingMerges.iterator().next();
        } else if (sourceTeamName.isEmpty()) {
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_merge", TeamCommandsUtils.getCommandRoot());
        } else {
            source = TeamManager.getTeamByName(sourceTeamName);
            if (source == null || !pendingMerges.contains(source))
                return error(sender, "gtnhlib.chat.teams.error.no_merge_request_specific", sourceTeamName);
        }

        TeamActions.onMergeAccept(source, target, false, null);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMergeDeny(ICommandSender sender, String sourceTeamName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team target = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!target.isOwner(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.not_owner_merge_response");

        Set<Team> pendingMerges = TeamManager.getPendingMergeRequests(target);
        if (pendingMerges == null || pendingMerges.isEmpty())
            return error(sender, "gtnhlib.chat.teams.error.no_merge_request");

        Team source;
        if (pendingMerges.size() == 1) {
            source = pendingMerges.iterator().next();
        } else if (sourceTeamName.isEmpty()) {
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_merge", TeamCommandsUtils.getCommandRoot());
        } else {
            source = TeamManager.getTeamByName(sourceTeamName);
            if (source == null || !pendingMerges.contains(source))
                return error(sender, "gtnhlib.chat.teams.error.no_merge_request_specific", sourceTeamName);
        }

        TeamActions.onMergeDeny(player, source, target);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeDisband(ICommandSender sender) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;
        UUID playerId = player.getUniqueID();

        Team team = TeamManager.getTeamByPlayer(playerId);
        if (!team.isOwner(playerId)) {
            return error(sender, "gtnhlib.chat.teams.error.not_owner_disband");
        }
        if (!team.canBeDisbanded()) {
            return error(sender, "gtnhlib.chat.teams.error.last_owner_disband");
        }

        TeamActions.onDisband(team, false, null);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeHelp(ICommandSender sender) {
        if (GTNHLib.isMui2Loaded) {
            sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.gui"));
        }
        String root = TeamCommandsUtils.getCommandRoot();
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.1", GTNHLibConfig.teamSystemName));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.2", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.3", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.4", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.5", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.6", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.7", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.8", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.9", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.10", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.11", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.12", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.13", root));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.14", root));
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestTeamMembers(ICommandSender sender,
            SuggestionsBuilder builder) {
        if (!(sender instanceof EntityPlayer player)) return builder.buildFuture();
        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return builder.buildFuture();

        for (UUID memberUuid : team.getMembers()) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
            if (member != null) {
                builder.suggest(member.getCommandSenderName());
            } else {
                String cachedName = UsernameCache.getLastKnownUsername(memberUuid);
                if (cachedName != null) builder.suggest(cachedName);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPendingInvites(ICommandSender sender,
            SuggestionsBuilder builder) {
        if (!(sender instanceof EntityPlayer player)) return builder.buildFuture();
        Set<Team> invites = TeamManager.getPendingInvites(player.getUniqueID());
        if (invites == null) return builder.buildFuture();

        for (Team team : invites) builder.suggest(team.getTeamName());
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestOtherTeams(ICommandSender sender, SuggestionsBuilder builder) {
        if (!(sender instanceof EntityPlayer player)) return builder.buildFuture();
        Team ownTeam = TeamManager.getTeamByPlayer(player.getUniqueID());

        for (Team team : TeamManager.TEAMS) {
            if (team != ownTeam) builder.suggest(team.getTeamName());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPendingMerges(ICommandSender sender,
            SuggestionsBuilder builder) {
        if (!(sender instanceof EntityPlayer player)) return builder.buildFuture();
        Team target = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (target == null) return builder.buildFuture();

        Set<Team> pending = TeamManager.getPendingMergeRequests(target);
        if (pending == null) return builder.buildFuture();

        for (Team source : pending) builder.suggest(source.getTeamName());
        return builder.buildFuture();
    }

    private static void sendUsage(ICommandSender sender) {
        ChatComponentTranslation msg = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.usage",
                GTNHLibConfig.teamSystemName);
        msg.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(msg);
    }

}
