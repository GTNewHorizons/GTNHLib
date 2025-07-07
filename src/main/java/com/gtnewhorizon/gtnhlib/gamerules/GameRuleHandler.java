package com.gtnewhorizon.gtnhlib.gamerules;

import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class GameRuleHandler {

    public static final GameRuleHandler INSTANCE = new GameRuleHandler();

    private static final Object2ObjectMap<String, IGameRule> gameRulesMap = new Object2ObjectOpenHashMap<>();

    @SubscribeEvent
    public void loadWorldEvent(WorldEvent.Load event) {
        if (!event.world.isRemote) {
            for (IGameRule rule : GameRuleHandler.gameRulesMap.values()) {
                if (!event.world.getGameRules().hasRule(rule.getName())) {
                    event.world.getGameRules().setOrCreateGameRule(rule.getName(), rule.defaultValue());
                }
            }
        }
    }

    public static void registerGameRule(IGameRule rule) {
        if (gameRulesMap.containsKey(rule.getName())) {
            throw new RuntimeException("Duplicate GameRule Name: " + rule.getName());
        }
        gameRulesMap.put(rule.getName(), rule);
    }

    public static void notifyGameRuleUpdate(String name, String value) {
        IGameRule rule = gameRulesMap.get(name);
        if (rule != null) {
            rule.onValueUpdated(value);
        }
    }
}
