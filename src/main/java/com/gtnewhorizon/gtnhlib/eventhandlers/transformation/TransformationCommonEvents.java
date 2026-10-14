package com.gtnewhorizon.gtnhlib.eventhandlers.transformation;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.transformation.TransformationAnnouncePacket;
import com.gtnewhorizon.gtnhlib.transformation.TransformationClientState;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@EventBusSubscriber
public class TransformationCommonEvents {

    @SubscribeEvent
    public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof final EntityPlayer player) {
            player.registerExtendedProperties(TransformationProperty.BASE_KEY, new TransformationProperty(player));
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
                    data.updatePlayerSize(form);

                    final float eyeHeight = TransformationClientState.getModifiedEyeHeight(player, form);

                    player.eyeHeight = eyeHeight - player.yOffset;
                } else {
                    data.updatePlayerSize(ITransformation.DEFAULT_WIDTH, ITransformation.DEFAULT_HEIGHT);
                    player.eyeHeight = player.getDefaultEyeHeight();
                }
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
    public static void onPlayerClone(PlayerEvent.Clone event) {
        final TransformationProperty original = TransformationProperty.get(event.original);
        final TransformationProperty clone = TransformationProperty.get(event.entityPlayer);

        if (original != null && clone != null) {
            final ITransformation form = original.getTransformation();

            if (!event.wasDeath) {
                clone.setTransformation(form);
            } else if (form.retainFormOnDeath()) {
                final NBTTagCompound tag = new NBTTagCompound();
                original.saveNBTData(tag);
                clone.loadNBTData(tag);
            }
        }
    }
}
