package com.gtnewhorizon.gtnhlib.eventhandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.transformation.TransformationAnnouncePacket;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@EventBusSubscriber
public class TransformationEventHandler {

    public static float renderYOffset = 0.0f;

    private static float previousEyeHeight = 0.0f;
    private static boolean isShifted = false;

    @SubscribeEvent
    public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof final EntityPlayer player) {
            player.registerExtendedProperties(TransformationProperty.BASE_KEY, new TransformationProperty(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.wasDeath) {
            final ITransformation originalForm = TransformationProperty.getTransformation(event.original);
            final TransformationProperty clone = TransformationProperty.get(event.entityPlayer);

            if (originalForm != null & clone != null) {
                clone.setTransformation(originalForm);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!event.world.isRemote && event.entity instanceof final EntityPlayerMP player) {
            final ITransformation tf = TransformationProperty.getTransformation(player);
            if (tf != null) {
                NetworkHandler.instance.sendTo(new TransformationAnnouncePacket(player, tf), player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            final EntityPlayer player = event.player;
            final TransformationProperty data = TransformationProperty.get(player);

            if (data != null) {
                final ITransformation form = data.getTransformation();
                if (form != null) {
                    form.onTick(player);
                    data.updatePlayerSize(form.getWidth(), form.getHeight());

                    final float eyeHeight = getEyeHeight(player, form);

                    player.eyeHeight = eyeHeight - player.yOffset;
                } else {
                    data.updatePlayerSize(ITransformation.DEFAULT_WIDTH, ITransformation.DEFAULT_HEIGHT);
                    player.eyeHeight = player.getDefaultEyeHeight();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.thePlayer;

        if (player != null && mc.theWorld != null) {
            switch (event.phase) {
                case START -> {
                    final ITransformation tf = TransformationProperty.getTransformation(player);
                    if (tf != null) {
                        final float targetEyeHeight = getEyeHeight(player, tf);
                        final float defaultEyeHeight = player.getDefaultEyeHeight();

                        renderYOffset = player.yOffset - targetEyeHeight + defaultEyeHeight;

                        previousEyeHeight = player.eyeHeight;
                        player.eyeHeight = defaultEyeHeight;

                        player.lastTickPosY -= renderYOffset;
                        player.prevPosY -= renderYOffset;
                        player.posY -= renderYOffset;
                        isShifted = true;
                    }
                }
                case END -> {
                    if (isShifted) {
                        player.lastTickPosY += renderYOffset;
                        player.prevPosY += renderYOffset;
                        player.posY += renderYOffset;
                        player.eyeHeight += previousEyeHeight;
                        renderYOffset = 0.0f;
                        isShifted = false;
                    }
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        final ITransformation tf = TransformationProperty.getTransformation(Minecraft.getMinecraft().thePlayer);

        if (tf != null && tf.renderHands()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.target instanceof final EntityPlayer targetPlayer) {
            final ITransformation tf = TransformationProperty.getTransformation(targetPlayer);
            if (tf != null) {
                NetworkHandler.instance.sendTo(
                    new TransformationAnnouncePacket(targetPlayer, tf),
                    (EntityPlayerMP) event.entityPlayer
                );
            }
        }
    }

    private static float getEyeHeight(final EntityPlayer player, final ITransformation form) {
        float eyeHeight = form.getEyeHeight();
        if (player.isPlayerSleeping()) {
            eyeHeight = 0.2f;
        } else if (player.isSneaking()) {
            eyeHeight -= 0.08f * (form.getHeight() / ITransformation.DEFAULT_HEIGHT);
        }
        return eyeHeight;
    }
}
