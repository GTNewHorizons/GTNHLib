package com.gtnewhorizon.gtnhlib.integration.mui2;

public enum ScreenType {

    // I thought really hard about this naming scheme guys
    INVALID("gtnhlib.gui.teams.select_screen.invalid"),
    TEAM_LIST("gtnhlib.gui.teams.select_screen.team_list"),
    PLAYER_LIST("gtnhlib.gui.teams.select_screen.player_list"),
    INVITE_PLAYERS("gtnhlib.gui.teams.select_screen.invite"),
    TEAMS_INVITING_PLAYER("gtnhlib.gui.teams.select_screen.get_invites"),
    REQUEST_CONSUME("gtnhlib.gui.teams.select_screen.team_merge"),
    VIEW_CONSUMPTION_REQUESTS("gtnhlib.gui.teams.select_screen.get_team_merge");

    public final String langKey;

    public static final ScreenType[] types = values();

    ScreenType(String langKey) {
        this.langKey = langKey;
    }

    public boolean isViewToggleOf(ScreenType other) {
        return switch (this) {
            case INVITE_PLAYERS -> other == TEAMS_INVITING_PLAYER;
            case TEAMS_INVITING_PLAYER -> other == INVITE_PLAYERS;
            case REQUEST_CONSUME -> other == VIEW_CONSUMPTION_REQUESTS;
            case VIEW_CONSUMPTION_REQUESTS -> other == REQUEST_CONSUME;
            default -> false;
        };
    }
}
