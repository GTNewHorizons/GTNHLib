package com.gtnewhorizon.gtnhlib.teams;

import static com.gtnewhorizon.gtnhlib.util.CommandUtils.colorChatComponent;
import static com.gtnewhorizon.gtnhlib.util.CommandUtils.success;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.teams.TeamDataSync;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;

public class TeamActions {

    public static void onRename(Team team, String oldName, String newName, boolean adminAction,
            @Nullable ICommandSender admin) {
        team.markDirty();
        TeamManager.forEachOnlineTeamMember(team, member -> {
            NetworkHandler.instance.sendTo(TeamNetwork.createTeamInfoSyncPacket(member.getUniqueID()), member);
            success(
                    member,
                    adminAction ? "gtnhlib.chat.teams.message.admin_renamed_team"
                            : "gtnhlib.chat.teams.message.renamed_team",
                    colorChatComponent(EnumChatFormatting.GOLD, newName));
        });
        if (adminAction) {
            success(
                    admin,
                    "gtnhlib.chat.teams.admin.message.renamed",
                    colorChatComponent(EnumChatFormatting.GOLD, oldName),
                    colorChatComponent(EnumChatFormatting.GOLD, newName));
        }
    }

    public static void onInvite(Team team, EntityPlayer source, EntityPlayer target) {
        TeamManager.addPendingInvite(target.getUniqueID(), team);

        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.received_invite",
                colorChatComponent(EnumChatFormatting.GOLD, source.getCommandSenderName()),
                colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()),
                colorChatComponent(
                        EnumChatFormatting.YELLOW,
                        TeamCommandsUtils.getCommandRoot() + " accept \"" + team.getTeamName() + "\""),
                colorChatComponent(
                        EnumChatFormatting.YELLOW,
                        TeamCommandsUtils.getCommandRoot() + " deny \"" + team.getTeamName() + "\""));
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);
        target.addChatMessage(notification);

        success(
                source,
                "gtnhlib.chat.teams.message.sent_invite",
                colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(target)));
    }

    public static void onCancelInvite(Team team, UUID target) {
        TeamManager.removePendingInvite(target, team);
    }

    public static void onAccept(Team invitingTeam, EntityPlayer player) {
        // Leave current team first. If the team would be disbanded, merge it into the new team automatically.
        UUID playerId = player.getUniqueID();
        Team oldTeam = TeamManager.getTeamByPlayer(playerId);
        assert oldTeam != null;
        if (oldTeam.getMembers().size() == 1) {
            TeamManager.mergeTeams(invitingTeam, oldTeam);
        } else {
            TeamManager.transferTeamData(oldTeam, invitingTeam, playerId, TeamDataTransferReason.JoinedExistingTeam);
            oldTeam.removeMember(playerId);
            oldTeam.markDirty();
            TeamDataSync oldTeamData = TeamNetwork.createCompleteTeamDataSyncPacket(oldTeam);
            TeamManager.forEachOnlineTeamMember(oldTeam, member -> {
                if (member.getUniqueID().equals(playerId)) return;
                NetworkHandler.instance.sendTo(oldTeamData, member);
                success(
                        member,
                        "gtnhlib.chat.teams.message.other_left_team",
                        colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(player)));
            });
            invitingTeam.addMember(playerId);
        }
        TeamManager.removeAllPendingInvites(playerId);
        TeamManager.PLAYER_TEAM_CACHE.put(playerId, invitingTeam);
        invitingTeam.markDirty();

        TeamDataSync newTeamData = TeamNetwork.createCompleteTeamDataSyncPacket(invitingTeam);
        TeamManager.forEachOnlineTeamMember(invitingTeam, member -> {
            NetworkHandler.instance.sendTo(newTeamData, member);
            if (member.getUniqueID().equals(playerId)) {
                NetworkHandler.instance
                        .sendTo(TeamNetwork.createTeamInfoSyncPacket(player.getUniqueID()), (EntityPlayerMP) player);
                success(
                        member,
                        "gtnhlib.chat.teams.message.joined_team",
                        colorChatComponent(EnumChatFormatting.GOLD, invitingTeam.getTeamName()));
            } else {
                success(
                        member,
                        "gtnhlib.chat.teams.message.other_joined_team",
                        colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(player)));
            }
        });
    }

    public static void onDeny(Team team, EntityPlayer player) {
        TeamManager.removePendingInvite(player.getUniqueID(), team);
        success(
                player,
                "gtnhlib.chat.teams.message.declined_invite",
                colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()));
    }

    public static void onKick(Team team, UUID kicked, boolean adminAction, @Nullable ICommandSender admin) {
        team.removeMember(kicked);

        Team newTeam = TeamManager.createTeam(ServerPlayerUtils.getPlayerName(kicked), kicked);
        TeamManager.transferTeamData(team, newTeam, kicked, TeamDataTransferReason.JoinedNewTeam);
        team.markDirty();
        newTeam.markDirty();
        TeamDataSync teamData = TeamNetwork.createCompleteTeamDataSyncPacket(team);
        TeamManager.forEachOnlineTeamMember(team, member -> {
            NetworkHandler.instance.sendTo(teamData, member);
            success(
                    member,
                    adminAction ? "gtnhlib.chat.teams.message.admin_other_kicked_from_team"
                            : "gtnhlib.chat.teams.message.other_kicked_from_team",
                    colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(kicked)));
        });

        TeamManager.forEachOnlineTeamMember(newTeam, member -> {
            if (member.getUniqueID().equals(kicked)) {
                TeamNetwork.sendPlayerAllTeamData(member, newTeam);
                success(
                        member,
                        adminAction ? "gtnhlib.chat.teams.message.admin_kicked_from_team"
                                : "gtnhlib.chat.teams.message.kicked_from_team",
                        colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()));
            }
        });

        if (adminAction && admin != null) {
            success(
                    admin,
                    "gtnhlib.chat.admin.message.kicked",
                    colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(kicked)),
                    colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()));
        }
    }

    public static void onLeave(EntityPlayer player) {
        UUID playerId = player.getUniqueID();
        Team oldTeam = TeamManager.getTeamByPlayer(playerId);
        assert oldTeam != null;
        String teamName = oldTeam.getTeamName();
        oldTeam.removeMember(playerId);

        if (oldTeam.getMembers().isEmpty()) {
            TeamManager.TEAMS.remove(oldTeam);
            TeamManager.TEAM_MAP.remove(oldTeam.getTeamId());
            oldTeam.markRemoved();
        } else {
            TeamDataSync oldTeamData = TeamNetwork.createCompleteTeamDataSyncPacket(oldTeam);
            TeamManager.forEachOnlineTeamMember(oldTeam, member -> {
                NetworkHandler.instance.sendTo(oldTeamData, member);
                success(
                        member,
                        "gtnhlib.chat.teams.message.other_left_team",
                        colorChatComponent(EnumChatFormatting.GOLD, ServerPlayerUtils.getPlayerName(player)));
            });
        }

        // Create a new solo team for the player
        Team newTeam = TeamManager.createTeam(player.getCommandSenderName(), player.getUniqueID());
        TeamManager.transferTeamData(oldTeam, newTeam, playerId, TeamDataTransferReason.JoinedNewTeam);
        if (!oldTeam.getMembers().isEmpty()) oldTeam.markDirty();
        newTeam.markDirty();
        TeamNetwork.sendPlayerAllTeamData((EntityPlayerMP) player, newTeam);

        success(player, "gtnhlib.chat.teams.message.left_team", colorChatComponent(EnumChatFormatting.GOLD, teamName));
    }

    public static void onPromote(Team team, UUID target, boolean adminAction, @Nullable ICommandSender admin) {
        ChatComponentText playerComp = colorChatComponent(
                EnumChatFormatting.GOLD,
                ServerPlayerUtils.getPlayerName(target));
        if (team.isOfficer(target)) {
            team.addOwner(target);
            TeamManager.forEachOnlineTeamMember(team, member -> {
                success(member, "gtnhlib.chat.teams.message.promoted_to_owner", playerComp);
                if (member.getUniqueID().equals(target)) {
                    NetworkHandler.instance.sendTo(TeamNetwork.createTeamInfoSyncPacket(target), member);
                }
            });
            if (adminAction) {
                success(
                        admin,
                        "gtnhlib.chat.teams.admin.message.promoted_to_owner",
                        playerComp,
                        colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()));
            }
        } else {
            team.addOfficer(target);
            TeamManager.forEachOnlineTeamMember(team, member -> {
                success(member, "gtnhlib.chat.teams.message.promoted_to_officer", playerComp);
                if (member.getUniqueID().equals(target)) {
                    NetworkHandler.instance.sendTo(TeamNetwork.createTeamInfoSyncPacket(target), member);
                }
            });
            if (adminAction) {
                success(
                        admin,
                        "gtnhlib.chat.teams.admin.message.promoted_to_officer",
                        playerComp,
                        colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()));
            }
        }
    }

    public static void onDemote(Team team, UUID target, boolean adminAction, @Nullable ICommandSender admin) {
        ChatComponentText playerComp = colorChatComponent(
                EnumChatFormatting.GOLD,
                ServerPlayerUtils.getPlayerName(target));
        if (team.isOwner(target)) {
            team.removeOwner(target);
            TeamManager.forEachOnlineTeamMember(team, member -> {
                success(member, "gtnhlib.chat.teams.message.demoted_to_officer", playerComp);
                if (member.getUniqueID().equals(target)) {
                    NetworkHandler.instance.sendTo(TeamNetwork.createTeamInfoSyncPacket(target), member);
                }
            });
            if (adminAction) {
                success(
                        admin,
                        "gtnhlib.chat.teams.admin.message.demoted_to_officer",
                        playerComp,
                        colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()));
            }
        } else {
            team.removeOfficer(target);
            TeamManager.forEachOnlineTeamMember(team, member -> {
                success(member, "gtnhlib.chat.teams.message.demoted_to_member", playerComp);
                if (member.getUniqueID().equals(target)) {
                    NetworkHandler.instance.sendTo(TeamNetwork.createTeamInfoSyncPacket(target), member);
                }
            });
            if (adminAction) {
                success(
                        admin,
                        "gtnhlib.chat.teams.admin.message.demoted_to_member",
                        playerComp,
                        colorChatComponent(EnumChatFormatting.GOLD, team.getTeamName()));
            }
        }
    }

    public static void onMergeRequest(EntityPlayer player, Team source, Team target) {

        TeamManager.addPendingMergeRequest(source, target);

        ChatComponentText sourceComponent = colorChatComponent(EnumChatFormatting.GOLD, source.getTeamName());
        ChatComponentText targetComponent = colorChatComponent(EnumChatFormatting.GOLD, target.getTeamName());
        success(player, "gtnhlib.chat.teams.message.merge_request_sent", targetComponent);

        // Notify all online owners of the target team
        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.merge_request_received",
                sourceComponent,
                colorChatComponent(
                        EnumChatFormatting.YELLOW,
                        TeamCommandsUtils.getCommandRoot() + " merge accept \"" + source.getTeamName() + "\""),
                colorChatComponent(
                        EnumChatFormatting.YELLOW,
                        TeamCommandsUtils.getCommandRoot() + " merge deny \"" + source.getTeamName() + "\""));
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);
        Set<UUID> owners = target.getOwners();
        TeamManager.forEachOnlineTeamMember(target, member -> {
            if (owners.contains(member.getUniqueID())) {
                member.addChatMessage(notification);
            }
        });
    }

    public static void onMergeCancel(EntityPlayer player, Team source, Team target) {
        TeamManager.removePendingMergeRequest(source, target);

        ChatComponentText targetComponent = colorChatComponent(EnumChatFormatting.GOLD, target.getTeamName());
        success(player, "gtnhlib.chat.teams.message.merge_request_cancelled", targetComponent);
    }

    public static void onMergeAccept(Team source, Team target, boolean adminAction, @Nullable ICommandSender admin) {
        ChatComponentText sourceComponent = colorChatComponent(EnumChatFormatting.GOLD, source.getTeamName());
        ChatComponentText targetComponent = colorChatComponent(EnumChatFormatting.GOLD, target.getTeamName());

        // Capture member list before merge for notification purposes
        List<UUID> allMembers = new ArrayList<>(source.getMembers());
        allMembers.addAll(target.getMembers());

        if (!adminAction) {
            TeamManager.removePendingMergeRequest(source, target);
        }
        TeamManager.mergeTeams(target, source);

        ChatComponentTranslation notification = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.merge_complete",
                sourceComponent,
                targetComponent);
        notification.getChatStyle().setColor(EnumChatFormatting.GREEN);

        TeamDataSync dataPacket = TeamNetwork.createCompleteTeamDataSyncPacket(target);
        TeamManager.forEachOnlineTeamMember(target, member -> {
            NetworkHandler.instance.sendTo(TeamNetwork.createTeamInfoSyncPacket(member.getUniqueID()), member);
            NetworkHandler.instance.sendTo(dataPacket, member);
            member.addChatMessage(notification);
        });

        if (adminAction) {
            success(admin, "gtnhlib.chat.teams.admin.message.merged", sourceComponent, targetComponent);
        }
    }

    public static void onMergeDeny(EntityPlayer player, Team source, Team target) {
        TeamManager.removePendingMergeRequest(source, target);

        ChatComponentText sourceComponent = new ChatComponentText(source.getTeamName());
        sourceComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        success(player, "gtnhlib.chat.teams.message.merge_denied", sourceComponent);
    }

    public static void onDisband(Team team, boolean adminAction, @Nullable ICommandSender admin) {
        List<UUID> members = new ArrayList<>(team.getMembers());
        String teamName = team.getTeamName();

        TeamManager.TEAMS.remove(team);
        TeamManager.TEAM_MAP.remove(team.getTeamId());
        TeamManager.PENDING_MERGE_REQUESTS.remove(team);
        TeamManager.PENDING_MERGE_REQUESTS.values().forEach(teamSet -> teamSet.remove(team));
        team.markRemoved();
        for (Set<Team> teams : TeamManager.PENDING_INVITES.values()) {
            teams.remove(team);
        }

        ChatComponentTranslation notice = new ChatComponentTranslation(
                adminAction ? "gtnhlib.chat.teams.admin.message.team_disbanded"
                        : "gtnhlib.chat.teams.message.team_disbanded",
                colorChatComponent(EnumChatFormatting.GOLD, teamName));
        notice.getChatStyle().setColor(EnumChatFormatting.RED);

        for (UUID uuid : members) {
            String name = ServerPlayerUtils.getPlayerName(uuid);
            Team newTeam = TeamManager.getOrCreateTeam(name, uuid);
            TeamManager.transferTeamData(team, newTeam, uuid, TeamDataTransferReason.JoinedNewTeam);
            newTeam.markDirty();
            TeamManager.forEachOnlineTeamMember(newTeam, member -> {
                NetworkHandler.instance.sendTo(TeamNetwork.createTeamInfoSyncPacket(member.getUniqueID()), member);
                TeamNetwork.sendPlayerAllTeamData(member, newTeam);
                member.addChatMessage(notice);
            });
        }
        if (adminAction) {
            success(
                    admin,
                    "gtnhlib.chat.teams.admin.message.disbanded",
                    colorChatComponent(EnumChatFormatting.GOLD, teamName));
        }
    }
}
