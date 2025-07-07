package com.gtnewhorizon.gtnhlib.gamerules;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class GameRuleHandler {

    private static final Object2ObjectMap<String, IGameRule> gameRulesMap = new Object2ObjectOpenHashMap<>();

    public static void registerGameRule(IGameRule rule) {
        if (gameRulesMap.containsKey(rule.getName())) {
            throw new RuntimeException("Duplicate GameRule Name: " + rule.getName());
        }
        gameRulesMap.put(rule.getName(), rule);
    }

    public static void notifyGameRuleUpdate(String name, String value) {
        IGameRule rule = gameRulesMap.get(name);
        if (rule != null) {
            rule.onValueUpdated(value, Boolean.parseBoolean(value));
        }
    }

    public static Map<String, IGameRule> getGameRulesMap() {
        return gameRulesMap;
    }
}
