package com.gtnewhorizon.gtnhlib.integration.mui2.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.gtnewhorizon.gtnhlib.GTNHLib;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.value.sync.AbstractGenericSyncValue;
import com.gtnewhorizon.gtnhlib.integration.mui2.DisplayItem;
import com.gtnewhorizon.gtnhlib.integration.mui2.DisplayItemType;
import com.gtnewhorizon.gtnhlib.integration.mui2.GuiUtils;
import com.gtnewhorizon.gtnhlib.integration.mui2.TeamGui;
import com.gtnewhorizon.gtnhlib.integration.mui2.TeamGuiData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;

public class DisplayListSyncValue extends AbstractGenericSyncValue<DisplayListSync, DisplayListSyncValue> {

    public DisplayListSyncValue(TeamGuiData data, Consumer<DisplayListSync> clientSetter) {
        super(
                DisplayListSync.class,
                () -> null,
                clientSetter,
                () -> new DisplayListSync(data.forceRefreshWithNextUpdate, getDisplayListAndResetRefresh(data)),
                newValue -> {});
    }

    private static List<DisplayItem> getDisplayListAndResetRefresh(TeamGuiData data) {
        List<DisplayItem> displayItems = new ArrayList<>();
        Team viewTeam = TeamManager.getTeamById(data.currentView.currentTeam());
        UUID currentPlayer = data.getPlayer().getUniqueID();
        Team currentPlayerTeam = TeamManager.getTeamByPlayer(currentPlayer);
        switch (data.currentView.type()) {
            case PLAYER_LIST:
                if (viewTeam == null) break;
                viewTeam.getMembers().stream()
                        .sorted(
                                Comparator.<UUID>comparingInt(id -> viewTeam.getRole(id).ordinal()).reversed()
                                        .thenComparing(ServerPlayerUtils::getPlayerName))
                        .forEach(
                                playerId -> displayItems.add(
                                        new DisplayItem(
                                                DisplayItemType.PLAYER,
                                                ServerPlayerUtils.getPlayerName(playerId),
                                                viewTeam.getRole(playerId),
                                                playerId,
                                                false)));
                break;
            case TEAM_LIST:
                TeamManager.getTeamMap().forEach(
                        (teamId, teamObj) -> displayItems.add(
                                new DisplayItem(
                                        DisplayItemType.TEAM,
                                        GuiUtils.clampString(teamObj.getTeamName(), TeamGui.MAX_DISPLAY_STRING_LENGTH),
                                        null,
                                        teamId,
                                        teamObj.canBeDisbanded())));
                displayItems.sort(Comparator.comparing(DisplayItem::text));
                break;
            case INVITE_PLAYERS:
                Set<UUID> teamPlayerList = viewTeam.getMembers();
                ServerPlayerUtils.forAllOnlinePlayers(player -> {
                    UUID playerId = player.getUniqueID();
                    if (!teamPlayerList.contains(playerId)) {
                        displayItems.add(
                                new DisplayItem(
                                        DisplayItemType.PLAYER,
                                        GuiUtils.clampString(
                                                ServerPlayerUtils.getPlayerName(playerId),
                                                TeamGui.MAX_DISPLAY_STRING_LENGTH),
                                        null,
                                        playerId,
                                        TeamManager.getPendingInvites(playerId).contains(viewTeam)));
                    }
                });
                displayItems.sort(Comparator.comparing(DisplayItem::flag).thenComparing(DisplayItem::text));
                break;
            case TEAMS_INVITING_PLAYER:
                boolean canAcceptInvites = currentPlayerTeam != null
                        && currentPlayerTeam.canPlayerAcceptInvites(currentPlayer);
                TeamManager.getPendingInvites(currentPlayer).stream().sorted(Comparator.comparing(Team::getTeamName))
                        .forEach(
                                t -> displayItems.add(
                                        new DisplayItem(
                                                DisplayItemType.TEAM,
                                                GuiUtils.clampString(
                                                        t.getTeamName(),
                                                        TeamGui.MAX_DISPLAY_STRING_LENGTH),
                                                null,
                                                t.getTeamId(),
                                                canAcceptInvites)));
                break;
            case REQUEST_CONSUME:
                TeamManager.getTeamMap().forEach(
                        (teamId, teamObj) -> {
                        if (teamObj.equals(currentPlayerTeam)) return;
                        displayItems.add(
                            new DisplayItem(
                                    DisplayItemType.TEAM,
                                    GuiUtils.clampString(teamObj.getTeamName(), TeamGui.MAX_DISPLAY_STRING_LENGTH),
                                    null,
                                    teamId,
                                    TeamManager.getPendingMergeRequests(teamObj).contains(currentPlayerTeam)));
                        });
                displayItems.sort(Comparator.comparing(DisplayItem::flag).reversed().thenComparing(DisplayItem::text));
                break;
            case VIEW_CONSUMPTION_REQUESTS:
                TeamManager.getPendingMergeRequests(currentPlayerTeam).stream()
                        .sorted(Comparator.comparing(Team::getTeamName)).forEach(
                                t -> displayItems.add(
                                        new DisplayItem(
                                                DisplayItemType.TEAM,
                                                GuiUtils.clampString(
                                                        t.getTeamName(),
                                                        TeamGui.MAX_DISPLAY_STRING_LENGTH),
                                                null,
                                                t.getTeamId(),
                                                false)));
                break;
            default:
                break;
        }

        data.forceRefreshWithNextUpdate = false;
        return displayItems;
    }

    @Override
    protected DisplayListSync createDeepCopyOf(DisplayListSync value) {
        return DisplayListSync.copyOf(value);
    }

    @Override
    protected boolean areEqual(DisplayListSync cachedValue, DisplayListSync newValue) {
        if (cachedValue == null || newValue == null) return cachedValue == newValue;
        return cachedValue.equalsIgnoreUnsetForceRefresh(newValue);
    }

    @Override
    protected void serialize(PacketBuffer buffer, DisplayListSync value) throws IOException {
        value.serializeInto(buffer);
    }

    @Override
    protected DisplayListSync deserialize(PacketBuffer buffer) throws IOException {
        return DisplayListSync.deserializeFrom(buffer);
    }
}
