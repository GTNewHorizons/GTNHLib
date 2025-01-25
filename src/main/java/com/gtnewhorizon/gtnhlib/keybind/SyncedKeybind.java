package com.gtnewhorizon.gtnhlib.keybind;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Server-backed keybindings, allowing you to read the state of a key press on the server per-player. <br>
 * <br>
 * Supports both:
 * <ul>
 * <li>"Key held" - Is this key currently held down by the player
 * <li>"Key pressed" - Listener event fired when the player clicks a key
 * </ul>
 *
 * @author serenibyss
 * @since 0.6.5
 */
@SuppressWarnings("unused")
@EventBusSubscriber(side = Side.CLIENT)
public final class SyncedKeybind {

    private static final Int2ObjectMap<SyncedKeybind> KEYBINDS = new Int2ObjectOpenHashMap<>();
    private static int syncIndex = 0;

    @SideOnly(Side.CLIENT)
    private KeyBinding keybinding;
    @SideOnly(Side.CLIENT)
    private int keyCode;
    @SideOnly(Side.CLIENT)
    private boolean isKeyDown;

    private final WeakHashMap<EntityPlayerMP, Boolean> mapping = new WeakHashMap<>();
    private final WeakHashMap<EntityPlayerMP, Set<IKeyPressedListener>> playerListeners = new WeakHashMap<>();
    private final Set<IKeyPressedListener> globalListeners = Collections.newSetFromMap(new WeakHashMap<>());

    // Doubly-wrapped supplier for client-side only type
    private SyncedKeybind(Supplier<Supplier<KeyBinding>> keybindingGetter) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.keybinding = keybindingGetter.get().get();
        }
        KEYBINDS.put(syncIndex++, this);
    }

    private SyncedKeybind(int keyCode) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.keyCode = keyCode;
        }
        KEYBINDS.put(syncIndex++, this);
    }

    private SyncedKeybind(String nameKey, String categoryKey, int keyCode) {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            this.keybinding = (KeyBinding) createKeyBinding(nameKey, categoryKey, keyCode);
        }
        KEYBINDS.put(syncIndex++, this);
    }

    /**
     * Create a Keybind wrapper around a Minecraft {@link KeyBinding}.
     *
     * @param mcKeybinding Doubly-wrapped supplier around a keybinding from
     *                     {@link net.minecraft.client.settings.GameSettings Minecraft.getMinecraft().gameSettings}.
     */
    public static SyncedKeybind createFromMC(Supplier<Supplier<KeyBinding>> mcKeybinding) {
        return new SyncedKeybind(mcKeybinding);
    }

    /**
     * Create a new Keybind for a specified key code.
     *
     * @param keyCode The key code.
     */
    public static SyncedKeybind create(int keyCode) {
        return new SyncedKeybind(keyCode);
    }

    /**
     * Create a new Keybind with server held and pressed syncing to server.<br>
     * Will automatically create a keybinding entry in the MC settings page.
     *
     * @param nameKey     Translation key for the keybinding name.
     * @param categoryKey Translation key for the keybinding options category.
     * @param keyCode     The key code, from {@link Keyboard}.
     */
    public static SyncedKeybind createConfigurable(String nameKey, String categoryKey, int keyCode) {
        return new SyncedKeybind(nameKey, categoryKey, keyCode);
    }

    /**
     * Check if a player is currently holding down this key.
     *
     * @param player The player to check.
     *
     * @return If the key is held.
     */
    public boolean isKeyDown(EntityPlayer player) {
        if (player.worldObj.isRemote) {
            if (keybinding != null) {
                return keybinding.getIsKeyPressed();
            }
            return Keyboard.isKeyDown(keyCode);
        }
        Boolean isKeyDown = mapping.get((EntityPlayerMP) player);
        return isKeyDown != null ? isKeyDown : false;
    }

    /**
     * Registers an {@link IKeyPressedListener} to this key, which will have its {@link IKeyPressedListener#onKeyPressed
     * onKeyPressed} method called when the provided player presses this key.
     *
     * @param player   The player who owns this listener.
     * @param listener The handler for the key clicked event.
     */
    public SyncedKeybind registerPlayerListener(EntityPlayerMP player, IKeyPressedListener listener) {
        Set<IKeyPressedListener> listenerSet = playerListeners
                .computeIfAbsent(player, k -> Collections.newSetFromMap(new WeakHashMap<>()));
        listenerSet.add(listener);
        return this;
    }

    /**
     * Remove a player's listener on this keybinding for a provided player.
     *
     * @param player   The player who owns this listener.
     * @param listener The handler for the key clicked event.
     */
    public void removePlayerListener(EntityPlayerMP player, IKeyPressedListener listener) {
        Set<IKeyPressedListener> listenerSet = playerListeners.get(player);
        if (listenerSet != null) {
            listenerSet.remove(listener);
        }
    }

    /**
     * Registers an {@link IKeyPressedListener} to this key, which will have its {@link IKeyPressedListener#onKeyPressed
     * onKeyPressed} method called when any player presses this key.
     *
     * @param listener The handler for the key clicked event.
     */
    public SyncedKeybind registerGlobalListener(IKeyPressedListener listener) {
        globalListeners.add(listener);
        return this;
    }

    /**
     * Remove a global listener on this keybinding.
     *
     * @param listener The handler for the key clicked event.
     */
    public void removeGlobalListener(IKeyPressedListener listener) {
        globalListeners.remove(listener);
    }

    static SyncedKeybind getFromSyncId(int id) {
        return KEYBINDS.get(id);
    }

    // Server-side indirection
    @SideOnly(Side.CLIENT)
    private Object createKeyBinding(String nameLangKey, String category, int button) {
        KeyBinding keybinding = new KeyBinding(nameLangKey, button, category);
        ClientRegistry.registerKeyBinding(keybinding);
        return keybinding;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Int2BooleanMap updatingKeyDown = new Int2BooleanOpenHashMap();
            for (var entry : KEYBINDS.int2ObjectEntrySet()) {
                SyncedKeybind keybind = entry.getValue();
                boolean previousKeyDown = keybind.isKeyDown;

                if (keybind.keybinding != null) {
                    keybind.isKeyDown = keybind.keybinding.getIsKeyPressed();
                } else {
                    keybind.isKeyDown = Keyboard.isKeyDown(keybind.keyCode);
                }

                if (previousKeyDown != keybind.isKeyDown) {
                    updatingKeyDown.put(entry.getIntKey(), keybind.isKeyDown);
                }
            }
            if (!updatingKeyDown.isEmpty()) {
                NetworkHandler.instance.sendToServer(new PacketKeyDown(updatingKeyDown));
            }
        }
    }

    // Updated by the packet handler
    void updateKeyDown(boolean keyDown, EntityPlayerMP player) {
        this.mapping.put(player, keyDown);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onInputEvent(InputEvent.KeyInputEvent event) {
        IntList updatingPressed = new IntArrayList();
        for (var entry : KEYBINDS.int2ObjectEntrySet()) {
            SyncedKeybind keybind = entry.getValue();
            if (keybind.keybinding != null && keybind.keybinding.isPressed()) {
                updatingPressed.add(entry.getIntKey());
            } else if (Keyboard.getEventKey() == keybind.keyCode) {
                updatingPressed.add(entry.getIntKey());
            }
        }
        if (!updatingPressed.isEmpty()) {
            NetworkHandler.instance.sendToServer(new PacketKeyPressed(updatingPressed));
        }
    }

    // Updated by the packet handler
    void onKeyPressed(EntityPlayerMP player) {
        // Player listeners
        Set<IKeyPressedListener> listenerSet = playerListeners.get(player);
        if (listenerSet != null && !listenerSet.isEmpty()) {
            for (IKeyPressedListener listener : listenerSet) {
                listener.onKeyPressed(player, this);
            }
        }
        // Global listeners
        for (IKeyPressedListener listener : globalListeners) {
            listener.onKeyPressed(player, this);
        }
    }
}
