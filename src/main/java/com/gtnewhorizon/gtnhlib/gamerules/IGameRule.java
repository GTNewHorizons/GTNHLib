package com.gtnewhorizon.gtnhlib.gamerules;

public interface IGameRule {

    String getName();

    String defaultValue();

    default String getValue() {
        return GameRuleHandler.getCachedValue(getName());
    }
}
