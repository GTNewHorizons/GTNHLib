package com.gtnewhorizon.gtnhlib.keybind;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Server-side listener interface for when a player presses a specific key.
 *
 * @author serenibyss
 * @since 0.6.5
 */
public interface IKeyPressedListener {

    /**
     * Called <strong>server-side only</strong> when a player presses a specified keybinding.
     *
     * @param player     The player who pressed the key.
     * @param keyPressed The key the player pressed.
     */
    void onKeyPressed(EntityPlayerMP player, SyncedKeybind keyPressed);
}
