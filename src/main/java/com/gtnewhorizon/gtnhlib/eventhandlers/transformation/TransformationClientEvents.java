package com.gtnewhorizon.gtnhlib.eventhandlers.transformation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderHandEvent;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.transformation.TransformationClientState;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

@EventBusSubscriber(side = Side.CLIENT)
public class TransformationClientEvents {

    private static float previousEyeHeight = 0.0f;

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.thePlayer;

        if (player != null && mc.theWorld != null) {
            switch (event.phase) {
                case START -> {
                    final ITransformation tf = TransformationProperty.getTransformation(player);
                    if (tf != null) {
                        final float targetEyeHeight = TransformationClientState.getModifiedEyeHeight(player, tf);
                        final float defaultEyeHeight = player.getDefaultEyeHeight();

                        final float renderYOffset = player.yOffset - targetEyeHeight + defaultEyeHeight;
                        TransformationClientState.setRenderYOffset(renderYOffset);

                        previousEyeHeight = player.eyeHeight;
                        player.eyeHeight = defaultEyeHeight;

                        player.lastTickPosY -= renderYOffset;
                        player.prevPosY -= renderYOffset;
                        player.posY -= renderYOffset;
                        TransformationClientState.setShifted(true);
                    }
                }
                case END -> {
                    if (TransformationClientState.isShifted()) {
                        final float offset = TransformationClientState.getRenderYOffset();

                        player.lastTickPosY += offset;
                        player.prevPosY += offset;
                        player.posY += offset;
                        player.eyeHeight += previousEyeHeight;

                        TransformationClientState.setRenderYOffset(0.0f);
                        TransformationClientState.setShifted(false);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        final ITransformation tf = TransformationProperty.getTransformation(Minecraft.getMinecraft().thePlayer);

        if (tf != null && tf.renderHands()) {
            event.setCanceled(true);
        }
    }
}
