package com.gtnewhorizon.gtnhlib.gamerules;

public interface IGameRule {

    String getName();

    String defaultValue();

    void onValueUpdated(String value);
}
