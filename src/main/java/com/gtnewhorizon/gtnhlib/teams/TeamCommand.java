package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.UsernameCache;

import com.gtnewhorizon.gtnhlib.brigadier.BrigadierApi;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public class TeamCommand {

    private static final String ARG_TEAM_NAME = "teamName";
    private static final String ARG_NEW_NAME = "newName";
    private static final String ARG_PLAYER = "player";

    public static void register() {
        BrigadierApi.getCommandDispatcher().register(literal("gtnhteam").executes(ctx -> {
            sendUsage(ctx.getSource());
            return Command.SINGLE_SUCCESS;
        }).then(
                literal("rename").then(
                        argument(ARG_NEW_NAME, StringArgumentType.greedyString()).executes(
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
                                argument(ARG_TEAM_NAME, StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> suggestPendingInvites(ctx.getSource(), builder))
                                        .executes(
                                                ctx -> executeAccept(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, ARG_TEAM_NAME)))))

                .then(
                        literal("deny").executes(ctx -> executeDeny(ctx.getSource(), "")).then(
                                argument(ARG_TEAM_NAME, StringArgumentType.greedyString())
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
                                                argument(ARG_TEAM_NAME, StringArgumentType.greedyString()).suggests(
                                                        (ctx, builder) -> suggestOtherTeams(ctx.getSource(), builder))
                                                        .executes(
                                                                ctx -> executeMergeRequest(
                                                                        ctx.getSource(),
                                                                        StringArgumentType
                                                                                .getString(ctx, ARG_TEAM_NAME)))))
                                .then(
                                        literal("accept").executes(ctx -> executeMergeAccept(ctx.getSource(), ""))
                                                .then(
                                                        argument(ARG_TEAM_NAME, StringArgumentType.greedyString())
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
                                                        argument(ARG_TEAM_NAME, StringArgumentType.greedyString())
                                                                .suggests(
                                                                        (ctx, builder) -> suggestPendingMerges(
                                                                                ctx.getSource(),
                                                                                builder))
                                                                .executes(
                                                                        ctx -> executeMergeDeny(
                                                                                ctx.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        ctx,
                                                                                        ARG_TEAM_NAME)))))));
    }

    private static int executeRename(ICommandSender sender, String newName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isTeamOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_rename");

        if (!team.renameTeam(newName)) {
            return error(sender, "gtnhlib.chat.teams.error.name_in_use");
        }

        ChatComponentText nameComponent = new ChatComponentText(newName);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.renamed_team", nameComponent);
    }

    private static int executeInvite(ICommandSender sender, String targetName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isTeamOfficer(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.not_officer_invite");

        EntityPlayer target = player.worldObj.getPlayerEntityByName(targetName);
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.not_online", targetName);
        if (team.isTeamMember(target.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.invite_teammate", targetName);
        if (target.getUniqueID().equals(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.invite_self");

        TeamManager.addPendingInvite(target.getUniqueID(), team);

        ChatComponentText nameComponent = new ChatComponentText(targetName);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        success(sender, "gtnhlib.chat.teams.message.sent_invite", nameComponent);

        ChatComponentText sentComponent = new ChatComponentText(player.getCommandSenderName());
        sentComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        ChatComponentText teamComponent = new ChatComponentText(team.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        ChatComponentText acceptComponent = new ChatComponentText("/gtnhteam accept " + team.getTeamName());
        acceptComponent.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        ChatComponentText denyComponent = new ChatComponentText("/gtnhteam deny " + team.getTeamName());
        denyComponent.getChatStyle().setColor(EnumChatFormatting.YELLOW);

        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.received_invite",
                sentComponent,
                teamComponent,
                acceptComponent,
                denyComponent);
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);
        target.addChatMessage(notification);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeAccept(ICommandSender sender, String teamName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Set<Team> invites = TeamManager.getPendingInvites(player.getUniqueID());
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
        Team currentTeam = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (currentTeam != null) {
            // Don't allow joining if player is sole owner of their team AND there are other members
            if (currentTeam.isTeamOwner(player.getUniqueID()) && currentTeam.getOwners().size() == 1
                    && currentTeam.getMembers().size() > 1) {
                return error(sender, "gtnhlib.chat.teams.error.last_owner_leave");
            }
            currentTeam.removeMember(player.getUniqueID());
            if (currentTeam.getMembers().isEmpty()) {
                TeamManager.mergeTeams(invitedTeam, currentTeam);
            }
        }

        invitedTeam.addMember(player.getUniqueID());
        TeamManager.removeAllPendingInvites(player.getUniqueID());
        TeamWorldSavedData.markForSaving();

        ChatComponentText teamComponent = new ChatComponentText(invitedTeam.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.joined_team", teamComponent);
    }

    private static int executeDeny(ICommandSender sender, String teamName) {
        EntityPlayer player = asPlayer(sender);
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

        ChatComponentText teamComponent = new ChatComponentText(specificTeam.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.declined_invite", teamComponent);
    }

    private static int executeLeave(ICommandSender sender) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        if (team.getMembers().size() == 1) {
            return error(sender, "gtnhlib.chat.teams.error.last_member_leave");
        }
        if (team.isTeamOwner(player.getUniqueID()) && team.getOwners().size() == 1) {
            return error(sender, "gtnhlib.chat.teams.error.last_owner_leave");
        }

        ChatComponentText teamComponent = new ChatComponentText(team.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);

        team.removeMember(player.getUniqueID());
        if (team.getMembers().isEmpty()) {
            TeamManager.TEAMS.remove(team);
            TeamWorldSavedData.markForSaving();
        }

        // Create a new solo team for the player
        TeamManager.getOrCreateTeam(player.getCommandSenderName(), player.getUniqueID());

        return success(sender, "gtnhlib.chat.teams.message.left_team", teamComponent);
    }

    private static int executePromote(ICommandSender sender, String targetName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isTeamOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_promote");

        UUID targetUuid = resolveTeamMemberUuid(team, targetName);
        if (targetUuid == null) return error(sender, "gtnhlib.chat.teams.error.other_not_in_team", targetName);
        if (team.isTeamOwner(targetUuid)) return error(sender, "gtnhlib.chat.teams.error.promote_owner", targetName);

        ChatComponentText nameComponent = new ChatComponentText(targetName);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        if (team.isTeamOfficer(targetUuid)) {
            team.addOwner(targetUuid);
            return success(sender, "gtnhlib.chat.teams.message.promoted_to_owner", nameComponent);
        } else {
            team.addOfficer(targetUuid);
            return success(sender, "gtnhlib.chat.teams.message.promoted_to_officer", nameComponent);
        }
    }

    private static int executeDemote(ICommandSender sender, String targetName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isTeamOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_demote");

        UUID targetUuid = resolveTeamMemberUuid(team, targetName);
        if (targetUuid == null) return error(sender, "gtnhlib.chat.teams.error.other_not_in_team", targetName);
        if (targetUuid.equals(player.getUniqueID()) && team.getOwners().size() == 1)
            return error(sender, "gtnhlib.chat.teams.error.last_owner_demote");
        if (!team.isTeamOfficer(targetUuid)) return error(sender, "gtnhlib.chat.teams.error.demote_member", targetName);

        ChatComponentText nameComponent = new ChatComponentText(targetName);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        if (team.isTeamOwner(targetUuid)) {
            team.removeOwner(targetUuid);
            return success(sender, "gtnhlib.chat.teams.message.demoted_to_officer", nameComponent);
        } else {
            team.removeOfficer(targetUuid);
            return success(sender, "gtnhlib.chat.teams.message.demoted_to_member", nameComponent);
        }
    }

    private static int executeInfo(ICommandSender sender) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "=== " + team.getTeamName() + " ==="));

        ChatComponentText ownersComponent = new ChatComponentText(formatUuidList(team.getOwners(), player.worldObj));
        ownersComponent.getChatStyle().setColor(EnumChatFormatting.WHITE);
        ChatComponentTranslation ownersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.owners",
                ownersComponent);
        ownersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(ownersTrans);

        ChatComponentText officersComponent = new ChatComponentText(
                formatUuidList(team.getOfficers(), player.worldObj));
        officersComponent.getChatStyle().setColor(EnumChatFormatting.WHITE);
        ChatComponentTranslation officersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.officers",
                officersComponent);
        officersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(officersTrans);

        ChatComponentText membersComponent = new ChatComponentText(formatUuidList(team.getMembers(), player.worldObj));
        membersComponent.getChatStyle().setColor(EnumChatFormatting.WHITE);
        ChatComponentTranslation membersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.members",
                membersComponent);
        membersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(membersTrans);

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMergeRequest(ICommandSender sender, String targetTeamName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team source = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (source == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!source.isTeamOwner(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.not_owner_merge_request");

        Team target = TeamManager.getTeamByName(targetTeamName);
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.team_not_found", targetTeamName);
        if (target == source) return error(sender, "gtnhlib.chat.teams.error.merge_self");

        if (TeamManager.hasPendingMergeRequest(source, target))
            return error(sender, "gtnhlib.chat.teams.error.merge_already_requested", targetTeamName);

        TeamManager.addPendingMergeRequest(source, target);

        ChatComponentText sourceComponent = new ChatComponentText(source.getTeamName());
        sourceComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        ChatComponentText targetComponent = new ChatComponentText(target.getTeamName());
        targetComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        success(sender, "gtnhlib.chat.teams.message.merge_request_sent", targetComponent);

        // Notify all online owners of the target team
        ChatComponentText acceptComponent = new ChatComponentText("/gtnhteam merge accept " + source.getTeamName());
        acceptComponent.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        ChatComponentText denyComponent = new ChatComponentText("/gtnhteam merge deny " + source.getTeamName());
        denyComponent.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.merge_request_received",
                sourceComponent,
                acceptComponent,
                denyComponent);
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);

        for (UUID ownerUuid : target.getOwners()) {
            EntityPlayer owner = sender.getEntityWorld().func_152378_a(ownerUuid); // getPlayerByUUID
            if (owner != null) owner.addChatMessage(notification);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMergeAccept(ICommandSender sender, String sourceTeamName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team target = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!target.isTeamOwner(player.getUniqueID()))
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

        ChatComponentText sourceComponent = new ChatComponentText(source.getTeamName());
        sourceComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        ChatComponentText targetComponent = new ChatComponentText(target.getTeamName());
        targetComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);

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
            EntityPlayer member = sender.getEntityWorld().func_152378_a(memberUuid); // getPlayerByUUID
            if (member != null) member.addChatMessage(notification);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeMergeDeny(ICommandSender sender, String sourceTeamName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team target = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!target.isTeamOwner(player.getUniqueID()))
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

    private static CompletableFuture<Suggestions> suggestTeamMembers(ICommandSender sender,
            SuggestionsBuilder builder) {
        if (!(sender instanceof EntityPlayer player)) return builder.buildFuture();
        Team team = TeamManager.getTeamByPlayer(player.getUniqueID());
        if (team == null) return builder.buildFuture();

        for (UUID memberUuid : team.getMembers()) {
            EntityPlayer member = sender.getEntityWorld().func_152378_a(memberUuid); // getPlayerByUUID
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

    private static EntityPlayer asPlayer(ICommandSender sender) {
        if (!(sender instanceof EntityPlayer player)) {
            sender.addChatMessage(new ChatComponentText("This command can only be used by a player."));
            return null;
        }
        return player;
    }

    private static int success(ICommandSender sender, String transKey, Object... args) {
        ChatComponentTranslation msg = new ChatComponentTranslation(transKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.GREEN);
        sender.addChatMessage(msg);
        return Command.SINGLE_SUCCESS;
    }

    private static int error(ICommandSender sender, String transKey, Object... args) {
        ChatComponentTranslation msg = new ChatComponentTranslation(transKey, args);
        msg.getChatStyle().setColor(EnumChatFormatting.RED);
        sender.addChatMessage(msg);
        return 0;
    }

    private static UUID resolveTeamMemberUuid(Team team, String name) {
        EntityPlayer online = MinecraftServer.getServer().getConfigurationManager().func_152612_a(name);
        if (online != null && team.isTeamMember(online.getUniqueID())) return online.getUniqueID();

        for (UUID uuid : team.getMembers()) {
            String cachedName = UsernameCache.getLastKnownUsername(uuid);
            if (cachedName != null && cachedName.equalsIgnoreCase(name)) return uuid;
        }
        return null;
    }

    private static String formatUuidList(java.util.List<UUID> uuids, net.minecraft.world.World world) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < uuids.size(); i++) {
            EntityPlayer p = world.func_152378_a(uuids.get(i)); // getPlayerByUUID
            if (p != null) {
                sb.append(p.getCommandSenderName());
            } else {
                String cachedName = UsernameCache.getLastKnownUsername(uuids.get(i));
                sb.append(cachedName == null ? uuids.get(i) : cachedName);
            }
            if (i < uuids.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private static LiteralArgumentBuilder<ICommandSender> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<ICommandSender, T> argument(String name,
            com.mojang.brigadier.arguments.ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
