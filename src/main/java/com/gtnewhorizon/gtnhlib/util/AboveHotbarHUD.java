package com.gtnewhorizon.gtnhlib.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AboveHotbarHUD {

    private static final AboveHotbarHUD instance = new AboveHotbarHUD();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static String displayText;
    private static int time;
    private static int ticks;
    private static boolean drawShadow;
    private static boolean shouldFade;

    /**
     * Used to register the values for rending text above the hotbar
     *
     * @param message      The message to display above the hotbar
     * @param duration     The duration to be displayed for in ticks
     * @param drawShadowIn Should the message be drawn with a drawShadow
     * @param shouldFadeIn Should the message fade away with time
     */
    public static void renderTextAboveHotbar(String message, int duration, boolean drawShadowIn, boolean shouldFadeIn) {
        displayText = message;
        ticks = duration;
        time = duration;
        drawShadow = drawShadowIn;
        shouldFade = shouldFadeIn;
        MinecraftForge.EVENT_BUS.register(instance);
        FMLCommonHandler.instance().bus().register(instance);
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void updateTicks(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (ticks < 0) {
                MinecraftForge.EVENT_BUS.unregister(instance);
                FMLCommonHandler.instance().bus().unregister(instance);
            }
            ticks--;
        }
    }

    /**
     * Renders text above the Hotbar of the player
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void renderTextAboveHotbar(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        int alpha = 1; // set to 1 just to pass the if(alpha > 0) check and render if shouldFade = false
        int color = 0;
        if (shouldFade) {
            alpha = ticks > time * 0.25F ? 255 : (int) (255F * ticks / (time * 0.25F));
            if (alpha < 5) {
                alpha = 0;
            }
            color = (alpha << 24);
        }
        if (alpha > 0) {
            GL11.glPushMatrix();
            {
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                final int x = (event.resolution.getScaledWidth() - mc.fontRenderer.getStringWidth(displayText)) / 2;
                final int y = event.resolution.getScaledHeight() - 70;
                mc.fontRenderer.drawString(displayText, x, y, color, drawShadow);
                GL11.glDisable(GL11.GL_BLEND);
            }
            GL11.glPopMatrix();
        }
    }
}
