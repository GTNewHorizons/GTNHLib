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

}
