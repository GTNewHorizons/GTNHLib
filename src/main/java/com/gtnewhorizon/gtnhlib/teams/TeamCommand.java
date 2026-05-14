package com.gtnewhorizon.gtnhlib.teams;

import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_NEW_NAME;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_PLAYER;
import static com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils.ARG_TEAM_NAME;
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
import java.util.concurrent.CompletableFuture;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.teams.TeamInfoSync;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public class TeamCommand {

    public static void register() {
        BrigadierApi.getCommandDispatcher().register(literal("gtnhteam").executes(ctx -> {
            sendUsage(ctx.getSource());
            return Command.SINGLE_SUCCESS;
        }).then(
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
                .then(literal("help").executes(ctx -> executeHelp(ctx.getSource()))));
    }

    private static int executeRename(ICommandSender sender, String newName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_rename");

        if (!team.renameTeam(newName)) {
            return error(sender, "gtnhlib.chat.teams.error.name_in_use");
        }

        TeamInfoSync packet = TeamNetwork.createTeamInfoSyncPacket(team);
        TeamManager.forEachOnlineTeamMember(team, member -> NetworkHandler.instance.sendTo(packet, member));

        for (UUID memberUuid : team.getMembers()) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
            if (member != null) success(
                    member,
                    "gtnhlib.chat.teams.message.renamed_team",
                    colorChatComponent(EnumChatFormatting.GOLD, newName));
        }

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

        TeamManager.addPendingInvite(target.getUniqueID(), team);

        success(
                sender,
                "gtnhlib.chat.teams.message.sent_invite",
                colorChatComponent(EnumChatFormatting.GOLD, targetName));

        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.received_invite",
                colorChatComponent(EnumChatFormatting.GOLD, player.getCommandSenderName()),
                colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()),
                colorChatComponent(EnumChatFormatting.YELLOW, "/gtnhteam accept \"" + team.getTeamName() + "\""),
                colorChatComponent(EnumChatFormatting.YELLOW, "/gtnhteam deny \"" + team.getTeamName() + "\""));
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);
        target.addChatMessage(notification);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAccept(ICommandSender sender, String teamName) {
        EntityPlayer player = TeamCommandsUtils.asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;
        UUID playerId = player.getUniqueID();

        Set<Team> invites = TeamManager.getPendingInvites(playerId);
        if (invites == null || invites.isEmpty()) return error(sender, "gtnhlib.chat.teams.error.no_invite");

        Team invitedTeam;
        if (invites.size() == 1) {
            invitedTeam = invites.iterator().next();
        } else if (teamName.isEmpty()) {
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_invite");
        } else {
            invitedTeam = TeamManager.getTeamByName(teamName);
            if (invitedTeam == null || !invites.contains(invitedTeam))
                return error(sender, "gtnhlib.chat.teams.error.no_invite_specific", teamName);
        }

        // Leave current team first. If the team would be disbanded, merge it into the new team automatically.
        Team currentTeam = TeamManager.getTeamByPlayer(playerId);
        if (currentTeam != null) {
            // Don't allow joining if player is sole owner of their team AND there are other members
            if (currentTeam.isOwner(playerId) && currentTeam.getOwners().size() == 1
                    && currentTeam.getMembers().size() > 1) {
                return error(sender, "gtnhlib.chat.teams.error.last_owner_leave");
            }
            currentTeam.removeMember(playerId);

            for (UUID memberUuid : currentTeam.getMembers()) {
                EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
                if (member != null) success(
                        member,
                        "gtnhlib.chat.teams.message.other_left_team",
                        colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(player)));
            }

            if (currentTeam.getMembers().isEmpty()) {
                TeamManager.mergeTeams(invitedTeam, currentTeam);
            } else {
                TeamManager.copyTeamData(currentTeam, invitedTeam, playerId, TeamDataCopyReason.JoinedExistingTeam);
            }
        }

        // Done before player is added to team so that they are not notified of their own join
        for (UUID memberUuid : invitedTeam.getMembers()) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
            if (member != null) success(
                    member,
                    "gtnhlib.chat.teams.message.other_joined_team",
                    colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(player)));
        }

        invitedTeam.addMember(playerId);
        TeamManager.removeAllPendingInvites(playerId);
        TeamNetwork.sendPlayerAllTeamData((EntityPlayerMP) player, invitedTeam);
        TeamManager.PLAYER_TEAM_CACHE.put(playerId, invitedTeam);
        invitedTeam.markDirty();

        return success(
                sender,
                "gtnhlib.chat.teams.message.joined_team",
                colorChatComponent(EnumChatFormatting.GOLD, invitedTeam.getTeamName()));
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
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_invite");
        } else {
            specificTeam = TeamManager.getTeamByName(teamName);
            if (specificTeam == null || !invites.contains(specificTeam))
                return error(sender, "gtnhlib.chat.teams.error.no_invite_specific", teamName);
        }

        TeamManager.removePendingInvite(player.getUniqueID(), specificTeam);

        return success(
                sender,
                "gtnhlib.chat.teams.message.declined_invite",
                colorChatComponent(EnumChatFormatting.GOLD, specificTeam.getTeamName()));
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

        String teamName = team.getTeamName();

        team.removeMember(playerId);
        if (team.getMembers().isEmpty()) {
            TeamManager.TEAMS.remove(team);
            TeamManager.TEAM_MAP.remove(team.getTeamId());
            team.markRemoved();
        }

        for (UUID memberUuid : team.getMembers()) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
            if (member != null) success(
                    member,
                    "gtnhlib.chat.teams.message.other_left_team",
                    colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(player)));
        }

        // Create a new solo team for the player
        Team newTeam = TeamManager.getOrCreateTeam(player.getCommandSenderName(), player.getUniqueID());
        TeamManager.copyTeamData(team, newTeam, playerId, TeamDataCopyReason.JoinedNewTeam);

        return success(
                sender,
                "gtnhlib.chat.teams.message.left_team",
                colorChatComponent(EnumChatFormatting.GOLD, teamName));
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

        if (team.isOfficer(targetUuid)) {
            team.addOwner(targetUuid);
            for (UUID memberUuid : team.getMembers()) {
                EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
                if (member != null) success(
                        member,
                        "gtnhlib.chat.teams.message.promoted_to_owner",
                        colorChatComponent(EnumChatFormatting.GOLD, targetName));
            }
        } else {
            team.addOfficer(targetUuid);
            for (UUID memberUuid : team.getMembers()) {
                EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
                if (member != null) success(
                        member,
                        "gtnhlib.chat.teams.message.promoted_to_officer",
                        colorChatComponent(EnumChatFormatting.GOLD, targetName));
            }
        }
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

        if (team.isOwner(targetUuid)) {
            team.removeOwner(targetUuid);
            for (UUID memberUuid : team.getMembers()) {
                EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
                if (member != null) success(
                        member,
                        "gtnhlib.chat.teams.message.demoted_to_officer",
                        colorChatComponent(EnumChatFormatting.GOLD, targetName));
            }
        } else {
            team.removeOfficer(targetUuid);
            for (UUID memberUuid : team.getMembers()) {
                EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
                if (member != null) success(
                        member,
                        "gtnhlib.chat.teams.message.demoted_to_member",
                        colorChatComponent(EnumChatFormatting.GOLD, targetName));
            }
        }
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

        TeamManager.addPendingMergeRequest(source, target);

        ChatComponentText sourceComponent = colorChatComponent(EnumChatFormatting.GOLD, source.getTeamName());
        ChatComponentText targetComponent = colorChatComponent(EnumChatFormatting.GOLD, target.getTeamName());

        success(sender, "gtnhlib.chat.teams.message.merge_request_sent", targetComponent);

        // Notify all online owners of the target team
        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.merge_request_received",
                sourceComponent,
                colorChatComponent(
                        EnumChatFormatting.YELLOW,
                        "/gtnhteam merge accept \"" + source.getTeamName() + "\""),
                colorChatComponent(EnumChatFormatting.YELLOW, "/gtnhteam merge deny \"" + source.getTeamName() + "\""));
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);

        for (UUID ownerUuid : target.getOwners()) {
            EntityPlayer owner = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), ownerUuid);
            if (owner != null) owner.addChatMessage(notification);
        }

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
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_merge");
        } else {
            source = TeamManager.getTeamByName(sourceTeamName);
            if (source == null || !pendingMerges.contains(source))
                return error(sender, "gtnhlib.chat.teams.error.no_merge_request_specific", sourceTeamName);
        }

        ChatComponentText sourceComponent = colorChatComponent(EnumChatFormatting.GOLD, source.getTeamName());
        ChatComponentText targetComponent = colorChatComponent(EnumChatFormatting.GOLD, target.getTeamName());

        // Capture member list before merge for notification purposes
        List<UUID> allMembers = new ArrayList<>(source.getMembers());
        allMembers.addAll(target.getMembers());

        TeamManager.removePendingMergeRequest(source, target);
        TeamManager.mergeTeams(target, source);

        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.merge_complete",
                sourceComponent,
                targetComponent);
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);

        for (UUID memberUuid : allMembers) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
            if (member != null) member.addChatMessage(notification);
        }

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
            return error(sender, "gtnhlib.chat.teams.error.disambiguate_merge");
        } else {
            source = TeamManager.getTeamByName(sourceTeamName);
            if (source == null || !pendingMerges.contains(source))
                return error(sender, "gtnhlib.chat.teams.error.no_merge_request_specific", sourceTeamName);
        }

        TeamManager.removePendingMergeRequest(source, target);

        ChatComponentText sourceComponent = new ChatComponentText(source.getTeamName());
        sourceComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.merge_denied", sourceComponent);
    }

    private static int executeHelp(ICommandSender sender) {
        if (GTNHLib.isMui2Loaded) {
            sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.gui"));
        }

        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.1"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.2"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.3"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.4"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.5"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.6"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.7"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.8"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.9"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.10"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.11"));
        sender.addChatMessage(new ChatComponentTranslation("gtnhlib.chat.teams.help.12"));
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestTeamMembers(ICommandSender sender,
            SuggestionsBuilder builder) {
        if (!(sender instanceof EntityPlayer player)) return builder.buildFuture();
        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return builder.buildFuture();

        for (UUID memberUuid : team.getMembers()) {
            EntityPlayer member = ServerPlayerUtils.getPlayerByUUID(sender.getEntityWorld(), memberUuid);
            if (member != null) builder.suggest(member.getCommandSenderName());
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
        ChatComponentTranslation msg = new ChatComponentTranslation("gtnhlib.chat.teams.message.usage");
        msg.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(msg);
    }

}
