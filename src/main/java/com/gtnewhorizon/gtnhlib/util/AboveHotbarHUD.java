package com.gtnewhorizon.gtnhlib.util;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class AboveHotbarHUD {

    private static final AboveHotbarHUD instance = new AboveHotbarHUD();

    @SubscribeEvent
    public void onDrawScreenPost(RenderGameOverlayEvent.Post event) {
        renderTextAboveHotbar();
    }

    @SubscribeEvent
    public void updateTicks(TickEvent.ClientTickEvent event) {
        if (ticks > 0 && event.phase == TickEvent.Phase.END) {
            ticks--;
        }
    }

    private static String displayText;
    private static int time;
    private static int ticks;
    private static boolean displayShadow;

    /**
     * Use when using custom colors for different parts of the text
     * @param text
     * Make sure you add Colour formatting, if not use displaySolidColor
     * @param duration
     * The duration is in ticks
     * @param shadow Asks if you want a shadow
     */
    public void display(String text, int duration, boolean shadow) {
        displayText = text;
        ticks = time = duration;
        displayShadow = shadow;
    }
    /**
     * Renders text above the Hotbar of the player
     */
    public static void renderTextAboveHotbar() {
        Minecraft mc = Minecraft.getMinecraft();
        int alpha = ticks > time * 0.25F ? 255 : (int) (255F * ticks / (time * 0.25F));
        if (alpha < 5) alpha = 0;
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int color = (alpha << 24);
        int x = res.getScaledWidth() / 2 - mc.fontRenderer.getStringWidth(displayText) / 2;
        int y = res.getScaledHeight() - 70;
        if (alpha > 0) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (displayShadow) mc.fontRenderer.drawStringWithShadow(displayText, x, y, color);
            else mc.fontRenderer.drawString(displayText, x, y, color);
            GL11.glDisable(GL11.GL_BLEND);
        }
        if (alpha == 0) {
            MinecraftForge.EVENT_BUS.unregister(instance);
            FMLCommonHandler.instance().bus().unregister(instance);
        }
    }

    /**
     * Used to register the values for rending text above the hotbar
     * @param message The message to display above the hotbar
     * @param duration The duration to be displayed for in ticks
     * @param shadow add a shadow on the text
     */
    public static void renderTextAboveHotbar(String message, int duration, boolean shadow) {
        instance.display(message, duration, shadow);
        MinecraftForge.EVENT_BUS.register(instance);
        FMLCommonHandler.instance().bus().register(instance);
    }
}
