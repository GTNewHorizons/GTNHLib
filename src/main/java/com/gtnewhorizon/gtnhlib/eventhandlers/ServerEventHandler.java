package com.gtnewhorizon.gtnhlib.eventhandlers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.UsernameCache;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.network.NetworkHandler;
import com.gtnewhorizon.gtnhlib.network.PlayerDataSync;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;
import com.gtnewhorizon.gtnhlib.teams.TeamNetwork;
import com.gtnewhorizon.gtnhlib.util.ServerPlayerUtils;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

@EventBusSubscriber
public class ServerEventHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerDataSync sync = new PlayerDataSync();
        sync.data.addAll(UsernameCache.getMap().values());
        ServerPlayerUtils.forAllOnlinePlayers(player -> NetworkHandler.instance.sendTo(sync, player));

        if (event.player instanceof EntityPlayerMP player) {
            Team team = TeamManager.getOrCreateTeam(player.getCommandSenderName(), player.getUniqueID());
            TeamNetwork.sendPlayerAllTeamData(player, team);
        }
    }
}
