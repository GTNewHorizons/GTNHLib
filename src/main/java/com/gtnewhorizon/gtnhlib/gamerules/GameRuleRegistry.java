package com.gtnewhorizon.gtnhlib.gamerules;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.GameRules;

public class GameRuleRegistry {

    private static final Map<String, IGameRule> gameRulesMap = new HashMap<>();

    /**
     * Registers a new {@link IGameRule} into the registry. New rules should be registered during the mod's
     * pre-initialization or initialization phase.
     *
     * @param rule The rule to be registered
     */
    public static void registerGameRule(IGameRule rule) {
        if (gameRulesMap.containsKey(rule.getName())) {
            throw new RuntimeException("Duplicate GameRule Name: " + rule.getName());
        }
        gameRulesMap.put(rule.getName(), rule);
    }

    /**
     * Internal method used by the registry. This should not be called from dependent mods. This method is triggered by
     * the underlying MC game rules system via a mixin to notify the registered {@link IGameRule} of updates to the
     * value.
     *
     * @param name      The name of the GameRule that has been updated.
     * @param value     The new value of the GameRule.
     * @param gameRules The underlying MC GameRules instance that was updated.
     */
    public static void notifyGameRuleUpdate(String name, String value, GameRules gameRules) {
        IGameRule rule = gameRulesMap.get(name);
        if (rule != null) {
            rule.onValueUpdated(value, Boolean.parseBoolean(value), gameRules);
        }
    }

    /**
     * Internal method used by the registry. This should be called from dependent mods. This method is triggered by the
     * underlying MC game rules system via a mixin to add all of the game rules from the registry into the underlying
     * system.
     *
     * @param gameRules The GameRules instance for our rules to be added to.
     */
    public static void injectGameRules(GameRules gameRules) {
        for (IGameRule rule : GameRuleRegistry.gameRulesMap.values()) {
            gameRules.addGameRule(rule.getName(), rule.defaultValue());
        }
    }
}
