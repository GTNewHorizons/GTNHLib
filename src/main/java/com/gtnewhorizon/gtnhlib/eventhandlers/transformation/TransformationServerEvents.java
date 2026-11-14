package com.gtnewhorizon.gtnhlib.eventhandlers.transformation;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;

import com.gtnewhorizon.gtnhlib.api.transformation.ITransformation;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.transformation.TransformationAnnouncePacket;
import com.gtnewhorizon.gtnhlib.transformation.TransformationProperty;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;

@EventBusSubscriber(side = Side.SERVER)
public class TransformationServerEvents {

    @SubscribeEvent
    public static void onStartTracking(StartTracking event) {
        if (event.target instanceof final EntityPlayer targetPlayer) {
            final ITransformation form = TransformationProperty.getTransformation(targetPlayer);
            if (form != null) {
                NetworkHandler.instance.sendTo(
                    new TransformationAnnouncePacket(targetPlayer, form),
                    (EntityPlayerMP) event.entityPlayer
                );
            }
        }
    }
}
