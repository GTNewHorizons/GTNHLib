package com.gtnewhorizon.gtnhlib.gamerules;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class GameRuleHandler {

    public static final GameRuleHandler INSTANCE = new GameRuleHandler();

    private static final Object2ObjectMap<String, String> gameRulesCache = new Object2ObjectOpenHashMap<>();
    private static final List<IGameRule> gameRules = new ArrayList<>();

    @SubscribeEvent
    public void loadWorldEvent(WorldEvent.Load event) {
        if (!event.world.isRemote) {
            for (IGameRule rule : GameRuleHandler.gameRules) {
                if (!event.world.getGameRules().hasRule(rule.getName())) {
                    event.world.getGameRules().addGameRule(rule.getName(), rule.defaultValue());
                }
            }
        }
    }

    public static void registerGameRule(IGameRule rule) {
        gameRulesCache.put(rule.getName(), rule.defaultValue());
        gameRules.add(rule);
    }

    public static String getCachedValue(String key) {
        return gameRulesCache.get(key);
    }

    public static boolean hasRule(String key) {
        return gameRulesCache.containsKey(key);
    }

    public static void updateCachedRule(String key, String value) {
        gameRulesCache.put(key, value);
    }
}
