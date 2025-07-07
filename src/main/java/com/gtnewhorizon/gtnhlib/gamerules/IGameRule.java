package com.gtnewhorizon.gtnhlib.gamerules;

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
     * locally cached value.
     *
     * @param value     The newly updated raw String value.
     * @param boolValue The String value parsed as a boolean.
     */
    void onValueUpdated(String value, boolean boolValue);

}
