package com.gtnewhorizon.gtnhlib.gamerules;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * This interface can be implemented and then registered with {@link GameRuleRegistry} to easily create a new game rule.
 */
public interface IGameRule {

    /**
     * @return The name of this game rule.
     */
    String getName();

    /**
     * @return The default value for this game rule. The rule will be set to this upon creation.
     */
    String defaultValue();

    /**
     * This method will be called when the underlying value of a GameRule is updated. This can be used to update a
     * locally cached value. By default this method will do nothing, in which case to know the value of the game rule
     * you will need to query it through traditional Minecraft/Forge means.
     *
     * @param value     The newly updated raw String value.
     * @param boolValue The String value parsed as a boolean.
     * @param gameRules The underlying MC GameRules instance was updated.
     */
    default void onValueUpdated(String value, boolean boolValue, GameRules gameRules) {}

    /**
     * Queries a value from the underlying MC GameRule's system. This is guaranteed to be the currently set value.
     *
     * @param world The world in which to make the GameRule query.
     * @return The current String value of the game rule.
     */
    default String queryStringValue(World world) {
        return world.getGameRules().getGameRuleStringValue(getName());
    }

    /**
     * Queries a value from the underlying MC GameRule's system. This is guaranteed to be the currently set value.
     *
     * @param world The world in which to make the GameRule query.
     * @return The current value of the game rule as a boolean.
     */
    default boolean queryBooleanValue(World world) {
        return world.getGameRules().getGameRuleBooleanValue(getName());
    }

}
