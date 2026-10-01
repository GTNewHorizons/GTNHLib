package com.gtnewhorizon.gtnhlib.integration.mui2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.integration.mui2.sync.DisplayListSyncValue;
import com.gtnewhorizon.gtnhlib.integration.mui2.sync.GuiViewSyncValue;
import com.gtnewhorizon.gtnhlib.integration.mui2.sync.TwoUuidActionSyncValue;
import com.gtnewhorizon.gtnhlib.integration.mui2.sync.UuidActionSyncValue;
import com.gtnewhorizon.gtnhlib.integration.mui2.sync.UuidStringActionSyncValue;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamActions;
import com.gtnewhorizon.gtnhlib.teams.TeamCommandsUtils;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.gtnewhorizon.gtnhlib.teams.TeamManagerClient;
import com.gtnewhorizon.gtnhlib.teams.TeamRole;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TeamGui implements IGuiHolder<TeamGuiData> {

    private static final int MAX_DISPLAY_LIST_SIZE = 256;
    public static final int MAX_DISPLAY_STRING_LENGTH = 25;
    private static final int LIST_ITEM_HEIGHT = 16;
    private static final int LIST_ITEM_WIDTH = 158;
    private static final int LIST_ACTION_BUTTON_SIZE = 12;
    private static final int LIST_ACTION_BUTTON_PADDING = 2;
    private static final int LIST_ACTION_BUTTON_POSITION_TOP = 2;
    private static final int[] LIST_ACTION_BUTTONS_POSITIONS_RIGHT = { 2, 14, 26 };
    private static final int PANEL_BUTTONS_ROW_POSITION_TOP = 20;
    private static final int PANEL_BUTTONS_SIZE = 16;
    private static final int[] PANEL_BUTTONS_POSITIONS_RIGHT = { 8, 26, 44, 62, 80, 98 };

    // Client-side only
    private static String screenTitle = "";
    protected static boolean forceRefresh = true;
    private static Stack<GuiView> windowHistory = new Stack<>();
    private static final List<DisplayItem> pendingDisplayList = new ArrayList<>();
    private static final List<DisplayItem> displayList = new ArrayList<>();
    private static ListWidget<IWidget, ?> displayListWidget;
    private static Team selectedTeam = null;
    private static boolean isFirstRequest = true;

    // Synced with server
    private boolean playerIsOp = false;

    @Override
    public ModularScreen createScreen(TeamGuiData data, ModularPanel mainPanel) {
        return new ModularScreen(GTNHLib.MODID, mainPanel);
    }

    @Override
    public ModularPanel buildUI(TeamGuiData data, PanelSyncManager syncManager, UISettings settings) {
        registerSyncValues(data, syncManager);

        if (data.isClient()) {
            resetDisplayState();
        }

        FreezablePanel panel = new FreezablePanel("gtnhlib:team_panel", this, data, syncManager);

        ConfirmationDialog confirmationDialog = new ConfirmationDialog("gtnhlib:confirmation_dialog");
        IPanelHandler confirmationPanel = IPanelHandler.simple(panel, (parent, player) -> confirmationDialog, true);
        TextDialog textDialog = new TextDialog("gtnhlib:text_dialog");
        IPanelHandler textDialogPanel = IPanelHandler.simple(panel, (parent, player) -> textDialog, true);

        panel.child(
                Flow.column().paddingLeft(5).paddingTop(5).height(16)
                        .child(createTitleBar(data, syncManager, textDialog, textDialogPanel)))
                .child(createPanelButtons(data, syncManager, confirmationDialog, confirmationPanel))
                .child(
                        createDisplayList(
                                data,
                                syncManager,
                                textDialog,
                                textDialogPanel,
                                confirmationDialog,
                                confirmationPanel))
                .child(ButtonWidget.panelCloseButton());
        return panel;
    }

    @SideOnly(Side.CLIENT)
    private static void resetDisplayState() {
        screenTitle = "";
        forceRefresh = true;
        windowHistory.clear();
        pendingDisplayList.clear();
        displayList.clear();
        selectedTeam = null;
        isFirstRequest = true;
    }

    private Flow createTitleBar(TeamGuiData data, PanelSyncManager syncManager, TextDialog textDialog,
            IPanelHandler textDialogPanel) {
        return Flow.row().child(
                new ButtonWidget<>().overlay(GuiTextures.EDIT)
                        .tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.edit_team_name")))
                        .setEnabledIf(
                                w -> data.currentView.type() == ScreenType.PLAYER_LIST
                                        && data.currentView.currentTeam().equals(TeamManagerClient.GetTeamId())
                                        && TeamManagerClient.getRole() == TeamRole.OWNER)
                        .onMouseTapped(mouseButton -> {
                            textDialog.setParams(
                                    StatCollector.translateToLocal("gtnhlib.gui.teams.edit_team_name"),
                                    TeamManagerClient.GetTeam().getTeamName(),
                                    newName -> {
                                        syncManager.findSyncHandler("edit_team_name", UuidStringActionSyncValue.class)
                                                .setValue(new ImmutablePair<>(null, newName));
                                    });
                            textDialogPanel.openPanel();
                            return true;
                        })
                        .setEnabledIf(
                                w -> data.currentView.type() == ScreenType.PLAYER_LIST
                                        && data.currentView.currentTeam().equals(TeamManagerClient.GetTeamId())
                                        && TeamManagerClient.getRole() == TeamRole.OWNER)
                        .size(14).marginRight(3).verticalCenter())
                .child(new TextWidget<>(IKey.dynamic(() -> screenTitle)).verticalCenter()).collapseDisabledChild();
    }

    private Flow createPanelButtons(TeamGuiData data, PanelSyncManager syncManager,
            ConfirmationDialog confirmationDialog, IPanelHandler confirmationPanel) {
        Flow panelButtons = Flow.row().top(PANEL_BUTTONS_ROW_POSITION_TOP);
        addPlayerListButtons(panelButtons, data, syncManager, confirmationDialog, confirmationPanel);
        addInviteButtons(panelButtons, data, syncManager);
        addMergeButtons(panelButtons, data, syncManager);
        return panelButtons;
    }

    private void addPlayerListButtons(Flow buttonFlow, TeamGuiData data, PanelSyncManager syncManager,
            ConfirmationDialog confirmationDialog, IPanelHandler confirmationPanel) {
        buttonFlow.child(
                new ButtonWidget<>().overlay(GuiTextures.ADD)
                        .tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.view_member_invites")))
                        .setEnabledIf(
                                w -> data.currentView.type() == ScreenType.PLAYER_LIST
                                        && data.currentView.currentTeam().equals(TeamManagerClient.GetTeamId()))
                        .onMouseTapped(mouseButton -> {
                            switchView(
                                    data,
                                    syncManager,
                                    new GuiView(ScreenType.INVITE_PLAYERS, data.currentView.currentTeam()));
                            return true;
                        }).size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[0]))
                .child(
                        new ButtonWidget<>().overlay(GuiTextures.MINIMIZE)
                                .tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.merge_team")))
                                .setEnabledIf(
                                        w -> data.currentView.type() == ScreenType.PLAYER_LIST
                                                && data.currentView.currentTeam().equals(TeamManagerClient.GetTeamId())
                                                && TeamManagerClient.getRole() == TeamRole.OWNER)
                                .onMouseTapped(mouseButton -> {
                                    switchView(
                                            data,
                                            syncManager,
                                            new GuiView(ScreenType.REQUEST_CONSUME, data.currentView.currentTeam()));
                                    return true;
                                }).size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[1]))
                .child(
                        new ButtonWidget<>().overlay(GuiTextures.MAXIMIZE)
                                .tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.disband_team")))
                                .setEnabledIf(
                                        w -> data.currentView.type() == ScreenType.PLAYER_LIST
                                                && data.currentView.currentTeam().equals(TeamManagerClient.GetTeamId())
                                                && TeamManagerClient.getRole() == TeamRole.OWNER)
                                .onMouseTapped(mouseButton -> {
                                    confirmationDialog.setParams(
                                            StatCollector.translateToLocal("gtnhlib.gui.teams.confirm_disband_team"),
                                            () -> {
                                                syncManager.findSyncHandler("request_disband", BooleanSyncValue.class)
                                                        .setValue(true);
                                            });
                                    confirmationPanel.openPanel();
                                    return true;
                                }).size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[2]))
                .child(
                        new ButtonWidget<>().overlay(GuiTextures.STOP)
                                .tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.leave_team")))
                                .setEnabledIf(
                                        w -> data.currentView.type() == ScreenType.PLAYER_LIST
                                                && data.currentView.currentTeam().equals(TeamManagerClient.GetTeamId()))
                                .onMouseTapped(mouseButton -> {
                                    confirmationDialog.setParams(
                                            StatCollector.translateToLocal("gtnhlib.gui.teams.confirm_leave_team"),
                                            () -> {
                                                syncManager.findSyncHandler("leave_team", BooleanSyncValue.class)
                                                        .setValue(true);
                                            });
                                    confirmationPanel.openPanel();
                                    return true;
                                }).size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[3]))
                .child(
                        new ButtonWidget<>().overlay(GuiTextures.ALL_DIRECTIONS)
                                .tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.view_all_teams")))
                                .setEnabledIf(w -> data.currentView.type() == ScreenType.PLAYER_LIST)
                                .onMouseTapped(mouseButton -> {
                                    switchView(
                                            data,
                                            syncManager,
                                            new GuiView(ScreenType.TEAM_LIST, data.currentView.currentTeam()));
                                    return true;
                                }).size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[4]))
                .child(
                        new ButtonWidget<>().overlay(GuiTextures.EDIT)
                                .tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.view_team_data")))
                                .setEnabledIf(w -> data.currentView.type() == ScreenType.PLAYER_LIST)
                                .onMouseTapped(mouseButton -> true).size(PANEL_BUTTONS_SIZE)
                                .right(PANEL_BUTTONS_POSITIONS_RIGHT[5]));

    }

    private void addInviteButtons(Flow buttonFlow, TeamGuiData data, PanelSyncManager syncManager) {
        buttonFlow
                .child(
                        createScreenTypeButton(ScreenType.INVITE_PLAYERS, data, syncManager)
                                .setEnabledIf(
                                        w -> data.currentView.type() == ScreenType.INVITE_PLAYERS
                                                || data.currentView.type() == ScreenType.TEAMS_INVITING_PLAYER)
                                .size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[0]))
                .child(
                        createScreenTypeButton(ScreenType.TEAMS_INVITING_PLAYER, data, syncManager)
                                .setEnabledIf(
                                        w -> data.currentView.type() == ScreenType.INVITE_PLAYERS
                                                || data.currentView.type() == ScreenType.TEAMS_INVITING_PLAYER)
                                .size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[1]));
    }

    private void addMergeButtons(Flow buttonFlow, TeamGuiData data, PanelSyncManager syncManager) {
        buttonFlow
                .child(
                        createScreenTypeButton(ScreenType.REQUEST_CONSUME, data, syncManager)
                                .setEnabledIf(
                                        w -> data.currentView.type() == ScreenType.REQUEST_CONSUME
                                                || data.currentView.type() == ScreenType.VIEW_CONSUMPTION_REQUESTS)
                                .size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[0]))
                .child(
                        createScreenTypeButton(ScreenType.VIEW_CONSUMPTION_REQUESTS, data, syncManager)
                                .setEnabledIf(
                                        w -> data.currentView.type() == ScreenType.REQUEST_CONSUME
                                                || data.currentView.type() == ScreenType.VIEW_CONSUMPTION_REQUESTS)
                                .size(PANEL_BUTTONS_SIZE).right(PANEL_BUTTONS_POSITIONS_RIGHT[1]));
    }

    private ToggleButton createScreenTypeButton(ScreenType type, TeamGuiData data, PanelSyncManager syncManager) {
        return new SelectButton().value(new BoolValue.Dynamic(() -> data.currentView.type() == type, value -> {
            GTNHLib.LOG.info("button for switching to {} pressed", type);
            switchView(data, syncManager, new GuiView(type, data.currentView.currentTeam()));
        })).size(16)
                .overlay(
                        type == ScreenType.INVITE_PLAYERS || type == ScreenType.REQUEST_CONSUME ? GuiTextures.UPLOAD
                                : GuiTextures.DOWNLOAD)
                .tooltipBuilder(tooltip -> tooltip.add(IKey.lang(type.langKey)));
    }

    private IWidget createDisplayList(TeamGuiData data, PanelSyncManager syncManager, TextDialog textDialog,
            IPanelHandler textDialogPanel, ConfirmationDialog confirmationDialog, IPanelHandler confirmationPanel) {

        EntityPlayer player = data.getPlayer();
        String currentPlayer = ServerPlayerUtils.getPlayerName(player);

        displayListWidget = new ListWidget<>().name("display_list").width(164).height(120)
                .background(CustomGuiTextures.TEXT_FIELD_BACKGROUND).horizontalCenter().top(40)
                .child(Flow.row().height(2));

        for (int i = 0; i < MAX_DISPLAY_LIST_SIZE; i++) {
            final int index = i;
            displayListWidget.child(
                    new ButtonWidget<>().size(LIST_ITEM_WIDTH, LIST_ITEM_HEIGHT)
                            .background(CustomGuiTextures.LIST_BACKGROUND).setEnabledIf(w -> index < displayList.size())
                            .onMouseTapped(mouseButton -> {
                                if (data.currentView.type() == ScreenType.TEAM_LIST) {
                                    if (!playerIsOp || selectedTeam != null
                                            && displayList.get(index).uuid().equals(selectedTeam.getTeamId())) {
                                        switchView(
                                                data,
                                                syncManager,
                                                new GuiView(ScreenType.PLAYER_LIST, displayList.get(index).uuid()));
                                    } else selectedTeam = new Team(
                                            displayList.get(index).text(),
                                            displayList.get(index).uuid(),
                                            true);
                                }
                                return true;
                            }).child(
                                    Flow.row()
                                            .child(
                                                    Flow.row().collapseDisabledChild()
                                                            .child(new TextWidget<>(IKey.dynamic(() -> {
                                                                if (index >= displayList.size()
                                                                        || displayList.get(index).role() == null)
                                                                    return "";
                                                                return switch (displayList.get(index).role()) {
                                                                    case OWNER -> " 2 ";
                                                                    case OFFICER -> " 1 ";
                                                                    case MEMBER -> " 0 ";
                                                                };
                                                            })).marginRight(3).setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.PLAYER_LIST))
                                                            .child(new TextWidget<>(IKey.dynamic(() -> {
                                                                if (index >= displayList.size()) {
                                                                    return "";
                                                                }
                                                                return displayList.get(index).text();
                                                            })).marginLeft(4)))
                                            // PLAYER_LIST
                                            .child(new ButtonWidget<>().overlay(GuiTextures.CLOSE).setEnabledIf(w -> {
                                                if (data.currentView.type() != ScreenType.PLAYER_LIST
                                                        || index >= displayList.size()
                                                        || currentPlayer.equals(displayList.get(index).text()))
                                                    return false;

                                                TeamRole displayRole = displayList.get(index).role();
                                                if (displayRole == null) return false;

                                                return !displayList.get(index).flag() && (this.playerIsOp
                                                        || TeamManagerClient.canPlayerKick(displayRole));
                                            }).onMouseTapped(mouseButton -> {
                                                UUID toKick = displayList.get(index).uuid();
                                                confirmationDialog.setParams(
                                                        StatCollector.translateToLocalFormatted(
                                                                TeamManagerClient
                                                                        .canPlayerKick(displayList.get(index).role())
                                                                                ? "gtnhlib.gui.teams.confirm_kick_member"
                                                                                : "gtnhlib.gui.teams.admin.confirm_kick_member",
                                                                displayList.get(index).text()),
                                                        () -> {
                                                            syncManager.findSyncHandler(
                                                                    "kick_member",
                                                                    UuidActionSyncValue.class).setValue(toKick);
                                                        });
                                                confirmationPanel.openPanel();
                                                return true;
                                            }).tooltip(t -> t.addLine(IKey.lang("gtnhlib.gui.teams.kick_member")))
                                                    .size(LIST_ACTION_BUTTON_SIZE)
                                                    .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[0])
                                                    .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                    .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.ARROW_DOWN)
                                                            .setEnabledIf(w -> {
                                                                if (data.currentView.type() != ScreenType.PLAYER_LIST
                                                                        || index >= displayList.size())
                                                                    return false;
                                                                TeamRole displayRole = displayList.get(index).role();
                                                                if (displayRole == null) return false;
                                                                return !displayList.get(index).flag()
                                                                        && displayRole.ordinal()
                                                                                > TeamRole.MEMBER.ordinal()
                                                                        && (this.playerIsOp || TeamManagerClient
                                                                                .canPlayerDemote(displayRole));
                                                            }).onMouseTapped(mouseButton -> {
                                                                UUID toDemote = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                TeamManagerClient.canPlayerDemote(
                                                                                        displayList.get(index).role())
                                                                                                ? "gtnhlib.gui.teams.confirm_demote_member"
                                                                                                : "gtnhlib.gui.teams.admin.confirm_demote_member",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "demote_member",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toDemote);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            })
                                                            .tooltip(
                                                                    t -> t.addLine(
                                                                            IKey.lang(
                                                                                    "gtnhlib.gui.teams.demote_member")))
                                                            .size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[1])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.ARROW_UP)
                                                            .setEnabledIf(w -> {
                                                                if (data.currentView.type() != ScreenType.PLAYER_LIST
                                                                        || index >= displayList.size())
                                                                    return false;
                                                                TeamRole displayRole = displayList.get(index).role();
                                                                if (displayRole == null) return false;
                                                                return displayRole.ordinal() < TeamRole.OWNER.ordinal()
                                                                        && (this.playerIsOp || TeamManagerClient
                                                                                .canPlayerPromote(displayRole));
                                                            }).onMouseTapped(mouseButton -> {
                                                                UUID toPromote = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                TeamManagerClient.canPlayerPromote(
                                                                                        displayList.get(index).role())
                                                                                                ? "gtnhlib.gui.teams.confirm_promote_member"
                                                                                                : "gtnhlib.gui.teams.admin.confirm_promote_member",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "promote_member",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toPromote);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            })
                                                            .tooltip(
                                                                    t -> t.addLine(
                                                                            IKey.lang(
                                                                                    "gtnhlib.gui.teams.promote_member")))
                                                            .size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[2])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            // TEAM_LIST Buttons
                                            .child(new ButtonWidget<>().overlay(GuiTextures.CLOSE).setEnabledIf(w -> {
                                                if (data.currentView.type() != ScreenType.TEAM_LIST
                                                        || index >= displayList.size()
                                                        || !displayList.get(index).flag())
                                                    return false;
                                                return this.playerIsOp;
                                            }).onMouseTapped(mouseButton -> {
                                                UUID toDisband = displayList.get(index).uuid();
                                                confirmationDialog.setParams(
                                                        StatCollector.translateToLocalFormatted(
                                                                "gtnhlib.gui.teams.admin.confirm_force_disband_team",
                                                                displayList.get(index).text()),
                                                        () -> {
                                                            syncManager
                                                                    .findSyncHandler(
                                                                            "force_request_disband",
                                                                            UuidActionSyncValue.class)
                                                                    .setValue(toDisband);
                                                        });
                                                confirmationPanel.openPanel();
                                                selectedTeam = null;
                                                return true;
                                            }).tooltip(
                                                    t -> t.addLine(
                                                            IKey.lang("gtnhlib.gui.teams.admin.force_disband_team")))
                                                    .size(LIST_ACTION_BUTTON_SIZE)
                                                    .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[0])
                                                    .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                    .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(new ButtonWidget<>().overlay(GuiTextures.EDIT).setEnabledIf(w -> {
                                                if (data.currentView.type() != ScreenType.TEAM_LIST
                                                        || index >= displayList.size())
                                                    return false;
                                                return this.playerIsOp;
                                            }).onMouseTapped(mouseButton -> {
                                                textDialog.setParams(
                                                        StatCollector.translateToLocal(
                                                                "gtnhlib.gui.teams.admin.force_edit_team_name"),
                                                        displayList.get(index).text(),
                                                        newName -> {
                                                            syncManager
                                                                    .findSyncHandler(
                                                                            "force_edit_team_name",
                                                                            UuidStringActionSyncValue.class)
                                                                    .setValue(
                                                                            new ImmutablePair<>(
                                                                                    displayList.get(index).uuid(),
                                                                                    newName));
                                                        });
                                                textDialogPanel.openPanel();
                                                return true;
                                            }).tooltip(
                                                    t -> t.addLine(
                                                            IKey.lang("gtnhlib.gui.teams.admin.force_edit_team_name")))
                                                    .size(LIST_ACTION_BUTTON_SIZE)
                                                    .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[1])
                                                    .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                    .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.MINIMIZE).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang(
                                                                            "gtnhlib.gui.teams.admin.force_merge_into_team")))
                                                            .setEnabledIf(w -> {
                                                                if (data.currentView.type() != ScreenType.TEAM_LIST
                                                                        || index >= displayList.size())
                                                                    return false;
                                                                return playerIsOp && selectedTeam != null
                                                                        && !selectedTeam.getTeamId()
                                                                                .equals(displayList.get(index).uuid());
                                                            }).onMouseTapped(mouseButton -> {
                                                                UUID surviving = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.admin.force_merge_confirmation",
                                                                                selectedTeam.getTeamName(),
                                                                                displayList.get(index).text(),
                                                                                selectedTeam.getTeamName()),
                                                                        () -> {
                                                                            syncManager.findSyncHandler(
                                                                                    "request_force_merge",
                                                                                    TwoUuidActionSyncValue.class)
                                                                                    .setValue(
                                                                                            new ImmutablePair<>(
                                                                                                    selectedTeam
                                                                                                            .getTeamId(),
                                                                                                    surviving));
                                                                            selectedTeam = null;
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[2])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            // INVITE_PLAYERS Buttons
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.CROSS).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang(
                                                                            "gtnhlib.gui.teams.cancel_invite_member")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.INVITE_PLAYERS
                                                                            && index < displayList.size()
                                                                            && TeamManagerClient
                                                                                    .doesPlayerSatisfyTeamRole(
                                                                                            TeamRole.OFFICER)
                                                                            && displayList.get(index).flag())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID toCancelInvite = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_cancel_invite_player",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "cancel_invite_player",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toCancelInvite);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[0])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.ADD).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang("gtnhlib.gui.teams.invite_member")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.INVITE_PLAYERS
                                                                            && index < displayList.size()
                                                                            && TeamManagerClient
                                                                                    .doesPlayerSatisfyTeamRole(
                                                                                            TeamRole.OFFICER)
                                                                            && !displayList.get(index).flag())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID toInvite = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_invite_player",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "invite_player",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toInvite);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[1])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            // TEAMS_INVITING_PLAYER Buttons
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.CROSS).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang(
                                                                            "gtnhlib.gui.teams.deny_team_invitation")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.TEAMS_INVITING_PLAYER
                                                                            && index < displayList.size())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID toDeny = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_deny_team_invitation",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "deny_team_invitation",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toDeny);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[0])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.FAVORITE).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang(
                                                                            "gtnhlib.gui.teams.accept_team_invitation")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.TEAMS_INVITING_PLAYER
                                                                            && index < displayList.size()
                                                                            && !displayList.get(index).flag())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID toAccept = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_accept_team_invitation",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "accept_team_invitation",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toAccept);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[1])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            // REQUEST_CONSUME buttons
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.CROSS).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang(
                                                                            "gtnhlib.gui.teams.cancel_merge_request")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.REQUEST_CONSUME
                                                                            && index < displayList.size()
                                                                            && TeamManagerClient
                                                                                    .doesPlayerSatisfyTeamRole(
                                                                                            TeamRole.OWNER)
                                                                            && displayList.get(index).flag())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID toCancelMerge = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_cancel_merge_request",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "cancel_merge_request",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toCancelMerge);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[0])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.ADD).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang("gtnhlib.gui.teams.request_merge")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.REQUEST_CONSUME
                                                                            && index < displayList.size()
                                                                            && TeamManagerClient
                                                                                    .doesPlayerSatisfyTeamRole(
                                                                                            TeamRole.OWNER)
                                                                            && !displayList.get(index).flag())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID mergeTarget = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_merge_request",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "request_merge",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(mergeTarget);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[1])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            // VIEW_CONSUMPTION_REQUESTS Buttons
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.CROSS).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang("gtnhlib.gui.teams.deny_merge_request")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.VIEW_CONSUMPTION_REQUESTS
                                                                            && index < displayList.size())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID toDeny = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_deny_merge_request",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "deny_merge_request",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toDeny);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[0])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))
                                            .child(
                                                    new ButtonWidget<>().overlay(GuiTextures.FAVORITE).tooltip(
                                                            t -> t.addLine(
                                                                    IKey.lang(
                                                                            "gtnhlib.gui.teams.accept_merge_request")))
                                                            .setEnabledIf(
                                                                    w -> data.currentView.type()
                                                                            == ScreenType.VIEW_CONSUMPTION_REQUESTS
                                                                            && index < displayList.size()
                                                                            && !displayList.get(index).flag())
                                                            .onMouseTapped(mouseButton -> {
                                                                UUID toAccept = displayList.get(index).uuid();
                                                                confirmationDialog.setParams(
                                                                        StatCollector.translateToLocalFormatted(
                                                                                "gtnhlib.gui.teams.confirm_accept_merge_request",
                                                                                displayList.get(index).text()),
                                                                        () -> {
                                                                            syncManager
                                                                                    .findSyncHandler(
                                                                                            "accept_merge_request",
                                                                                            UuidActionSyncValue.class)
                                                                                    .setValue(toAccept);
                                                                        });
                                                                confirmationPanel.openPanel();
                                                                return true;
                                                            }).size(LIST_ACTION_BUTTON_SIZE)
                                                            .right(LIST_ACTION_BUTTONS_POSITIONS_RIGHT[1])
                                                            .top(LIST_ACTION_BUTTON_POSITION_TOP)
                                                            .padding(LIST_ACTION_BUTTON_PADDING))

                            ));
        }

        return displayListWidget;
    }

    public static void updateDisplayList(boolean replaceAll) {
        if (replaceAll) {
            displayList.clear();
            displayList.addAll(pendingDisplayList);
        } else {
            // Update values in-place
            Map<UUID, DisplayItem> pendingMapping = new HashMap<>();
            pendingDisplayList.forEach(displayItem -> pendingMapping.put(displayItem.uuid(), displayItem));
            displayList.replaceAll(displayItem -> pendingMapping.getOrDefault(displayItem.uuid(), displayItem));
        }
    }

    private void registerSyncValues(TeamGuiData data, PanelSyncManager syncManager) {
        syncManager.syncValue("screen_title", new StringSyncValue(() -> screenTitle, newScreenTitle -> {
            screenTitle = GuiUtils.clampString(
                    data.currentView.type() == ScreenType.PLAYER_LIST ? newScreenTitle
                            : StatCollector.translateToLocal(newScreenTitle),
                    MAX_DISPLAY_STRING_LENGTH);
        }, () -> {
            if (data.currentView.type() == ScreenType.PLAYER_LIST) {
                Team team = TeamManager.getTeamById(data.currentView.currentTeam());
                if (team == null) return "";
                return team.getTeamName();
            }
            return data.currentView.type().langKey;
        }, newScreenTitle -> {}));

        syncManager.syncValue(
                "player_is_op",
                new BooleanSyncValue(() -> this.playerIsOp, playerIsOp -> this.playerIsOp = playerIsOp, () -> {
                    this.playerIsOp = GuiUtils.isOpServerSideOnly(data.getPlayer());
                    return this.playerIsOp;
                }, playerIsOp -> {}));

        syncManager.syncValue("team_gui_mode", new GuiViewSyncValue(() -> data.currentView, newView -> {
            data.currentView = newView;
            if (!data.isClient()) {
                data.forceRefreshWithNextUpdate = true;
            }
        }));

        syncManager.syncValue("team_gui_display", new DisplayListSyncValue(data, newList -> {
            pendingDisplayList.clear();
            pendingDisplayList.addAll(newList.data);
            forceRefresh = newList.forceRefresh || isFirstRequest;
            isFirstRequest = false;
        }));

        syncManager.syncValue("edit_team_name", new UuidStringActionSyncValue(request -> {
            if (request != null && request.getLeft() == null && // regular team name change ignores team uuid
            request.getRight() != null && request.getRight().length() <= Team.MAX_TEAM_NAME_LENGTH) {
                EntityPlayer player = data.getPlayer();
                Team target = TeamManager.getTeamByPlayer(player.getUniqueID());
                if (target == null) {
                    GTNHLib.LOG.error("Could not rename team because UUID {} is invalid", request.getLeft());
                    return;
                }

                String oldName = target.getTeamName();
                if (target.getOwners().contains(player.getUniqueID())) {
                    if (target.renameTeam(request.getRight())) {
                        TeamActions.onRename(target, oldName, request.getRight(), false, null);
                    } else {
                        GTNHLib.LOG.warn("Could not rename team {} as the new name was already in use.", oldName);
                    }
                } else {
                    GTNHLib.LOG.warn(
                            "Player {} is not allowed to rename team {}",
                            ServerPlayerUtils.getPlayerName(player),
                            target.getTeamName());
                }
            } else {
                GTNHLib.LOG.error(
                        "Received admin team name change request from non-admin player {}",
                        ServerPlayerUtils.getPlayerName(data.getPlayer().getUniqueID()));
            }
        }));

        syncManager.syncValue("force_edit_team_name", new UuidStringActionSyncValue(request -> {
            if (request != null && request.getLeft() != null
                    && request.getRight() != null
                    && request.getRight().length() <= Team.MAX_TEAM_NAME_LENGTH
                    && playerIsOp) {
                Team target = TeamManager.getTeamById(request.getLeft());

                if (target == null) {
                    GTNHLib.LOG.error("Could not force rename team because UUID {} is invalid", request.getLeft());
                    return;
                }
                String oldName = target.getTeamName();

                if (target.renameTeam(request.getRight())) {
                    TeamActions.onRename(target, oldName, request.getRight(), true, data.getPlayer());
                } else {
                    GTNHLib.LOG.warn("Could not force-rename team {} as the new name was already in use.", oldName);
                }
            } else {
                GTNHLib.LOG.warn(
                        "Player {} failed to force rename team",
                        ServerPlayerUtils.getPlayerName(data.getPlayer()));
            }
        }));

        syncManager.syncValue("kick_member", new UuidActionSyncValue(request -> {
            UUID requestingPlayer = data.getPlayer().getUniqueID();
            Team team = TeamManager.getTeamByPlayer(requestingPlayer);
            if (team == null || requestingPlayer.equals(request) || !team.getMembers().contains(request)) return;
            if (TeamCommandsUtils.canKick(team.getRole(requestingPlayer), team.getRole(request))) {
                TeamActions.onKick(team, request, false, null);
            } else if (playerIsOp) {
                if (team.getOwners().size() == 1 && team.getRole(request) == TeamRole.OWNER) return;
                TeamActions.onKick(team, request, true, data.getPlayer());
            } else {
                GTNHLib.LOG.warn(
                        "Player {} does not have permissions to kick {}",
                        ServerPlayerUtils.getPlayerName(requestingPlayer),
                        ServerPlayerUtils.getPlayerName(request));
            }
        }));

        syncManager.syncValue("promote_member", new UuidActionSyncValue(request -> {
            UUID requestingPlayer = data.getPlayer().getUniqueID();
            Team team = TeamManager.getTeamByPlayer(requestingPlayer);
            if (team == null || !team.getMembers().contains(requestingPlayer)
                    || !team.getMembers().contains(request)
                    || team.getRole(request) == TeamRole.OWNER)
                return;
            if (TeamCommandsUtils.canPromote(team.getRole(requestingPlayer), team.getRole(request))) {
                TeamActions.onPromote(team, request, false, null);
            } else if (playerIsOp) {
                TeamActions.onPromote(
                        team,
                        request,
                        true,
                        ServerPlayerUtils.getPlayerByUUID(data.getWorld(), requestingPlayer));
            } else {
                GTNHLib.LOG.warn(
                        "player {} does not have permissions to promote {}",
                        ServerPlayerUtils.getPlayerName(requestingPlayer),
                        ServerPlayerUtils.getPlayerName(request));
            }
        }));

        syncManager.syncValue("demote_member", new UuidActionSyncValue(request -> {
            UUID requestingPlayer = data.getPlayer().getUniqueID();
            Team team = TeamManager.getTeamByPlayer(requestingPlayer);
            if (team == null || !team.getMembers().contains(requestingPlayer)
                    || !team.getMembers().contains(request)
                    || team.getRole(request) == TeamRole.MEMBER)
                return;
            if (team.getOwners().size() == 1 && team.getRole(request) == TeamRole.OWNER) return;
            if (TeamCommandsUtils.canDemote(team.getRole(requestingPlayer), team.getRole(request))) {
                TeamActions.onDemote(team, request, false, null);
            } else if (playerIsOp) {
                TeamActions.onDemote(
                        team,
                        request,
                        true,
                        ServerPlayerUtils.getPlayerByUUID(data.getWorld(), requestingPlayer));
            } else {
                GTNHLib.LOG.warn(
                        "player {} does not have permissions to demote {}",
                        ServerPlayerUtils.getPlayerName(requestingPlayer),
                        ServerPlayerUtils.getPlayerName(request));
            }
        }));

        syncManager
                .syncValue("request_disband", new BooleanSyncValue(() -> false, request -> {}, () -> false, request -> {
                    UUID playerId = data.getPlayer().getUniqueID();
                    Team team = TeamManager.getTeamByPlayer(playerId);
                    if (request && team != null && team.isOwner(playerId) && team.getMembers().size() > 1) {
                        TeamActions.onDisband(team, false, null);
                    } else {
                        GTNHLib.LOG.warn(
                                "Player {} failed to disband their team",
                                ServerPlayerUtils.getPlayerName(data.getPlayer()));
                    }
                }).allowC2S());

        syncManager.syncValue("leave_team", new BooleanSyncValue(() -> false, request -> {}, () -> false, request -> {
            UUID playerId = data.getPlayer().getUniqueID();
            Team team = TeamManager.getTeamByPlayer(playerId);
            if (!request || team == null || team.getMembers().size() == 1) return;
            if (team.isOwner(playerId) && team.getOwners().size() == 1) return;

            TeamActions.onLeave(data.getPlayer());
        }).allowC2S());

        syncManager.syncValue("force_request_disband", new UuidActionSyncValue(request -> {
            Team team = TeamManager.getTeamById(request);
            if (playerIsOp && team != null && team.canBeDisbanded()) {
                TeamActions.onDisband(team, true, data.getPlayer());
            } else {
                GTNHLib.LOG.warn(
                        "Player {} failed to disband team {}",
                        ServerPlayerUtils.getPlayerName(data.getPlayer()),
                        team == null ? "" : team.getTeamName());
            }
        }));

        syncManager.syncValue("invite_player", new UuidActionSyncValue(request -> {
            UUID invitingPlayer = data.getPlayer().getUniqueID();
            if (invitingPlayer == null) return;

            EntityPlayer invitedPlayer = ServerPlayerUtils.getPlayerByUUID(data.getWorld(), request);
            if (invitedPlayer == null) return;

            Team team = TeamManager.getTeamByPlayer(invitingPlayer);
            if (team == null) return;

            if (!team.isOfficer(invitingPlayer)) {
                GTNHLib.LOG.warn(
                        "player {} does not have permissions to invite others to their team",
                        ServerPlayerUtils.getPlayerName(invitingPlayer));
                return;
            }
            TeamActions.onInvite(team, data.getPlayer(), invitedPlayer);
        }));

        syncManager.syncValue("cancel_invite_player", new UuidActionSyncValue(request -> {
            UUID invitingPlayer = data.getPlayer().getUniqueID();
            if (invitingPlayer == null) return;

            if (request == null) return;

            Team team = TeamManager.getTeamByPlayer(invitingPlayer);
            if (team == null) return;

            if (!team.isOfficer(invitingPlayer)) {
                GTNHLib.LOG.warn(
                        "player {} does not have permissions to cancel player invites",
                        ServerPlayerUtils.getPlayerName(invitingPlayer));
                return;
            }
            TeamActions.onCancelInvite(team, request);
        }));

        syncManager.syncValue("accept_team_invitation", new UuidActionSyncValue(request -> {
            UUID invitedPlayer = data.getPlayer().getUniqueID();
            Team currentTeam = TeamManager.getTeamByPlayer(invitedPlayer);
            Team invitingTeam = TeamManager.getTeamById(request);
            if (currentTeam == null || invitingTeam == null || currentTeam == invitingTeam) return;
            if (currentTeam.getOwners().contains(invitedPlayer) && currentTeam.getOwners().size() == 1
                    && currentTeam.getMembers().size() > 1)
                return;
            if (!TeamManager.getPendingInvites(invitedPlayer).contains(invitingTeam)) {
                ModularUI.LOGGER.info(
                        "No pending invite to player {} from team {}, cannot accept invitation",
                        data.getPlayer().getGameProfile().getName(),
                        invitingTeam.getTeamName());
                return;
            }
            TeamActions.onAccept(invitingTeam, data.getPlayer());
        }));

        syncManager.syncValue("deny_team_invitation", new UuidActionSyncValue(request -> {
            UUID invitedPlayer = data.getPlayer().getUniqueID();
            Team currentTeam = TeamManager.getTeamByPlayer(invitedPlayer);
            Team invitingTeam = TeamManager.getTeamById(request);
            if (currentTeam == null || invitingTeam == null || currentTeam == invitingTeam) return;
            if (!TeamManager.getPendingInvites(invitedPlayer).contains(invitingTeam)) {
                ModularUI.LOGGER.info(
                        "No pending invite to player {} from team {}, cannot deny invitation",
                        data.getPlayer().getGameProfile().getName(),
                        invitingTeam.getTeamName());
                return;
            }
            TeamActions.onDeny(invitingTeam, data.getPlayer());
        }));

        syncManager.syncValue("request_merge", new UuidActionSyncValue(request -> {
            Team surviving = TeamManager.getTeamById(request);
            UUID playerId = data.getPlayer().getUniqueID();
            Team consumed = TeamManager.getTeamByPlayer(playerId);

            if (surviving == null || consumed == null || surviving == consumed) return;
            if (consumed.isOwner(playerId)) {
                TeamActions.onMergeRequest(data.getPlayer(), consumed, surviving);
            } else {
                GTNHLib.LOG.warn(
                        "Player {} failed to request team merger",
                        ServerPlayerUtils.getPlayerName(data.getPlayer()));
            }
        }));

        syncManager.syncValue("cancel_merge_request", new UuidActionSyncValue(request -> {
            Team surviving = TeamManager.getTeamById(request);
            UUID playerId = data.getPlayer().getUniqueID();
            Team consumed = TeamManager.getTeamByPlayer(playerId);

            if (surviving == null || consumed == null || surviving == consumed) return;
            if (consumed.isOwner(playerId)) {
                TeamActions.onMergeCancel(data.getPlayer(), consumed, surviving);
            } else {
                GTNHLib.LOG.warn(
                        "Player {} failed to cancel merge request to team {}",
                        ServerPlayerUtils.getPlayerName(data.getPlayer()),
                        surviving.getTeamName());
            }
        }));

        syncManager.syncValue("accept_merge_request", new UuidActionSyncValue(request -> {
            Team consumed = TeamManager.getTeamById(request);
            UUID playerId = data.getPlayer().getUniqueID();
            Team surviving = TeamManager.getTeamByPlayer(playerId);

            if (surviving == null || consumed == null || surviving == consumed) return;
            if (!TeamManager.hasPendingMergeRequest(consumed, surviving)) return;
            if (surviving.isOwner(playerId)) {
                TeamActions.onMergeAccept(consumed, surviving, false, null);
            } else {
                GTNHLib.LOG.warn(
                        "Player {} failed to accept merge request",
                        ServerPlayerUtils.getPlayerName(data.getPlayer()));
            }
        }));

        syncManager.syncValue("deny_merge_request", new UuidActionSyncValue(request -> {
            Team consumed = TeamManager.getTeamById(request);
            UUID playerId = data.getPlayer().getUniqueID();
            Team surviving = TeamManager.getTeamByPlayer(playerId);

            if (surviving == null || consumed == null || surviving == consumed) return;
            if (!TeamManager.hasPendingMergeRequest(consumed, surviving)) return;
            if (surviving.isOwner(playerId)) {
                TeamActions.onMergeDeny(data.getPlayer(), consumed, surviving);
            } else {
                GTNHLib.LOG.warn(
                        "Player {} failed to deny merge request",
                        ServerPlayerUtils.getPlayerName(data.getPlayer()));
            }
        }));

        syncManager.syncValue("request_force_merge", new TwoUuidActionSyncValue(request -> {
            if (request != null && request.getRight() != null && request.getLeft() != null && playerIsOp) {
                Team surviving = TeamManager.getTeamById(request.getRight());
                Team consumed = TeamManager.getTeamById(request.getLeft());
                if (surviving == null || consumed == null || surviving == consumed) return;
                TeamActions.onMergeAccept(consumed, surviving, true, data.getPlayer());
            }
        }));

    }

    public void switchView(TeamGuiData data, PanelSyncManager syncManager, GuiView newView) {
        if (newView.equals(data.currentView)) {
            return;
        }
        if (!windowHistory.isEmpty() && windowHistory.peek().equals(newView)) {
            restoreView(data, syncManager);
            return;
        }
        windowHistory.push(data.currentView);
        data.currentView = newView;
        syncManager.findSyncHandler("team_gui_mode", GuiViewSyncValue.class).setValue(newView);
        selectedTeam = null;
        displayListWidget.getScrollArea().getScrollY().scrollTo(displayListWidget.getScrollArea(), 0);
    }

    public void restoreView(TeamGuiData data, PanelSyncManager syncManager) {
        if (windowHistory.isEmpty()) return;
        data.currentView = windowHistory.pop();
        syncManager.findSyncHandler("team_gui_mode", GuiViewSyncValue.class).setValue(data.currentView);
        selectedTeam = null;
        displayListWidget.getScrollArea().getScrollY().scrollTo(displayListWidget.getScrollArea(), 0);
    }

}
