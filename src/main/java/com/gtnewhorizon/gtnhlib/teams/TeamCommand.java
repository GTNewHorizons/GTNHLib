package com.gtnewhorizon.gtnhlib.teams;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

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
                literal("create").then(
                        argument(ARG_TEAM_NAME, StringArgumentType.greedyString()).executes(
                                ctx -> executeCreate(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, ARG_TEAM_NAME)))))

                .then(
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

                .then(literal("accept").executes(ctx -> executeAccept(ctx.getSource())))

                .then(literal("deny").executes(ctx -> executeDeny(ctx.getSource())))

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

                .then(literal("info").executes(ctx -> executeInfo(ctx.getSource()))));
    }

    private static int executeCreate(ICommandSender sender, String name) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        if (TeamManager.isInTeam(player.getUniqueID())) {
            return error(sender, "gtnhlib.chat.teams.error.already_in_team");
        }
        if (!TeamManager.isTeamNameValid(name)) {
            return error(sender, "gtnhlib.chat.teams.error.name_in_use");
        }

        Team team = new Team(name);
        team.initializeData(TeamDataRegistry.getRegisteredKeys().toArray(new String[0]));
        team.addOwner(player.getUniqueID());
        TeamManager.TEAMS.add(team);
        TeamWorldSavedData.markForSaving();

        ChatComponentText nameComponent = new ChatComponentText(name);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.created_team", nameComponent);
    }

    private static int executeRename(ICommandSender sender, String newName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeam(player.getUniqueID());
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

        Team team = TeamManager.getTeam(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isTeamOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_invite");

        EntityPlayer target = player.worldObj.getPlayerEntityByName(targetName);
        if (target == null) return error(sender, "gtnhlib.chat.teams.error.not_online", targetName);
        if (TeamManager.isInTeam(target.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.other_already_in_team", targetName);

        TeamManager.addPendingInvite(target.getUniqueID(), team);

        ChatComponentText nameComponent = new ChatComponentText(targetName);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        success(sender, "gtnhlib.chat.teams.message.sent_invite", nameComponent);

        ChatComponentText sentComponent = new ChatComponentText(player.getCommandSenderName());
        sentComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        ChatComponentText teamComponent = new ChatComponentText(team.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        ChatComponentText acceptComponent = new ChatComponentText("/gtnhteam accept");
        acceptComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        ChatComponentText denyComponent = new ChatComponentText("/gtnhteam deny");
        denyComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);

        ChatComponentTranslation mainComponent = new ChatComponentTranslation(
                "gtnhlib.chat.teams.message.received_invite",
                sentComponent,
                teamComponent,
                acceptComponent,
                denyComponent);
        mainComponent.getChatStyle().setColor(EnumChatFormatting.GREEN);

        target.addChatMessage(mainComponent);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeAccept(ICommandSender sender) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        if (TeamManager.isInTeam(player.getUniqueID()))
            return error(sender, "gtnhlib.chat.teams.error.already_in_team");

        Team team = TeamManager.getPendingInvite(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.no_invite");

        team.addMember(player.getUniqueID());
        TeamManager.removePendingInvite(player.getUniqueID());

        ChatComponentText teamComponent = new ChatComponentText(team.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.joined_team", teamComponent);
    }

    private static int executeDeny(ICommandSender sender) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getPendingInvite(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.no_invite");

        TeamManager.removePendingInvite(player.getUniqueID());

        ChatComponentText teamComponent = new ChatComponentText(team.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.declined_invite", teamComponent);
    }

    private static int executeLeave(ICommandSender sender) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeam(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        if (team.isTeamOwner(player.getUniqueID()) && team.getOwners().size() == 1 && team.getMembers().size() > 1) {
            return error(sender, "gtnhlib.chat.teams.error.last_owner_leave");
        }

        team.removeMember(player.getUniqueID());

        if (team.getMembers().isEmpty()) {
            TeamManager.TEAMS.remove(team);
            TeamWorldSavedData.markForSaving();
        }

        ChatComponentText teamComponent = new ChatComponentText(team.getTeamName());
        teamComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.left_team", teamComponent);
    }

    private static int executePromote(ICommandSender sender, String targetName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeam(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isTeamOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_promote");

        UUID targetUuid = resolveTeamMemberUuid(team, targetName);
        if (targetUuid == null) return error(sender, "gtnhlib.chat.teams.error.other_not_in_team", targetName);
        if (team.isTeamOwner(targetUuid)) return error(sender, "gtnhlib.chat.teams.error.promote_owner", targetName);

        team.addOwner(targetUuid);

        ChatComponentText nameComponent = new ChatComponentText(targetName);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.promoted", nameComponent);
    }

    private static int executeDemote(ICommandSender sender, String targetName) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeam(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");
        if (!team.isTeamOwner(player.getUniqueID())) return error(sender, "gtnhlib.chat.teams.error.not_owner_demote");

        UUID targetUuid = resolveTeamMemberUuid(team, targetName);
        if (targetUuid == null) return error(sender, "gtnhlib.chat.teams.error.other_not_in_team", targetName);
        if (targetUuid.equals(player.getUniqueID()) && team.getOwners().size() == 1)
            return error(sender, "gtnhlib.chat.teams.error.last_owner_demote");
        if (!team.isTeamOwner(targetUuid)) return error(sender, "gtnhlib.chat.teams.error.demote_member", targetName);

        team.removeOwner(targetUuid);

        ChatComponentText nameComponent = new ChatComponentText(targetName);
        nameComponent.getChatStyle().setColor(EnumChatFormatting.GOLD);
        return success(sender, "gtnhlib.chat.teams.message.demoted", nameComponent);
    }

    private static int executeInfo(ICommandSender sender) {
        EntityPlayer player = asPlayer(sender);
        if (player == null) return Command.SINGLE_SUCCESS;

        Team team = TeamManager.getTeam(player.getUniqueID());
        if (team == null) return error(sender, "gtnhlib.chat.teams.error.not_in_team");

        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "=== " + team.getTeamName() + " ==="));

        ChatComponentText ownersComponent = new ChatComponentText(formatUuidList(team.getOwners(), player.worldObj));
        ownersComponent.getChatStyle().setColor(EnumChatFormatting.WHITE);
        ChatComponentTranslation ownersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.owners",
                ownersComponent);
        ownersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(ownersTrans);

        ChatComponentText membersComponent = new ChatComponentText(formatUuidList(team.getMembers(), player.worldObj));
        membersComponent.getChatStyle().setColor(EnumChatFormatting.WHITE);
        ChatComponentTranslation membersTrans = new ChatComponentTranslation(
                "gtnhlib.chat.teams.info.members",
                membersComponent);
        membersTrans.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        sender.addChatMessage(membersTrans);

        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestTeamMembers(ICommandSender sender,
            SuggestionsBuilder builder) {

        if (!(sender instanceof EntityPlayer player)) return builder.buildFuture();
        Team team = TeamManager.getTeam(player.getUniqueID());
        if (team == null) return builder.buildFuture();

        for (UUID memberUuid : team.getMembers()) {
            EntityPlayer member = sender.getEntityWorld().func_152378_a(memberUuid); // getPlayerByUUID
            if (member != null) builder.suggest(member.getCommandSenderName());
        }
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

    /** Looks up a UUID for a player name among a team's current members, online or not. */
    private static UUID resolveTeamMemberUuid(Team team, String name) {
        for (UUID uuid : team.getMembers()) {
            EntityPlayer online = team.getMembers().isEmpty() ? null
                    : net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().func_152612_a(name); // getPlayerByUsername
            if (online != null && online.getUniqueID().equals(uuid)) return uuid;
        }
        return null;
    }

    private static String formatUuidList(java.util.List<UUID> uuids, net.minecraft.world.World world) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < uuids.size(); i++) {
            EntityPlayer p = world.func_152378_a(uuids.get(i));
            sb.append(p != null ? p.getCommandSenderName() : uuids.get(i).toString());
            if (i < uuids.size() - 1) sb.append(", ");
        }
        return sb.length() == 0 ? "(none)" : sb.toString();
    }

    private static LiteralArgumentBuilder<ICommandSender> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<ICommandSender, T> argument(String name,
            com.mojang.brigadier.arguments.ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
