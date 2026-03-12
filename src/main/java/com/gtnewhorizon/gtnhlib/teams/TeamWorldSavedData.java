package com.gtnewhorizon.gtnhlib.teams;

import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.WorldEvent;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

@EventBusSubscriber
public class TeamWorldSavedData extends WorldSavedData {

    public static TeamWorldSavedData INSTANCE;

    private static final String DATA_NAME = "GTNHLib_TeamWorldSavedData";

    private static void loadInstance(World world) {
        MapStorage storage = world.mapStorage;
        INSTANCE = (TeamWorldSavedData) storage.loadData(TeamWorldSavedData.class, DATA_NAME);
        if (INSTANCE == null) {
            INSTANCE = new TeamWorldSavedData();
            storage.setData(DATA_NAME, INSTANCE);
        }
        INSTANCE.markDirty();
    }

    public static void markForSaving() {
        if (INSTANCE != null) {
            // if at the time of calling, the instance is null, then there is no need to mark it dirty anyway
            INSTANCE.markDirty();
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
            loadInstance(event.world);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            TeamManager.getOrCreateTeam(player.getCommandSenderName(), player.getUniqueID());
        }
    }

    public TeamWorldSavedData() {
        super(DATA_NAME);
    }

    @SuppressWarnings("unused")
    public TeamWorldSavedData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound NBT) {
        TeamManager.clear();
        NBTTagList teamList = NBT.getTagList("TeamList", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < teamList.tagCount(); i++) {
            NBTTagCompound teamTag = teamList.getCompoundTagAt(i);
            String teamName = teamTag.getString("TeamName");

            Team team = new Team(teamName);

            // Owners
            NBTTagList ownersList = teamTag.getTagList("Owners", Constants.NBT.TAG_STRING);
            for (int j = 0; j < ownersList.tagCount(); j++) {
                team.addOwner(UUID.fromString(ownersList.getStringTagAt(j)));
            }

            // Officers
            NBTTagList officersList = teamTag.getTagList("Officers", Constants.NBT.TAG_STRING);
            for (int j = 0; j < officersList.tagCount(); j++) {
                team.addOfficer(UUID.fromString(officersList.getStringTagAt(j)));
            }

            // Members
            NBTTagList membersList = teamTag.getTagList("Members", Constants.NBT.TAG_STRING);
            for (int j = 0; j < membersList.tagCount(); j++) {
                team.addMember(UUID.fromString(membersList.getStringTagAt(j)));
            }

            NBTTagCompound teamData = teamTag.getCompoundTag("TeamData");
            for (String key : TeamDataRegistry.getRegisteredKeys()) {
                ITeamData data = TeamDataRegistry.construct(key);
                if (data != null && teamData.hasKey(key)) {
                    data.readFromNBT(teamData.getCompoundTag(key));
                    team.putData(key, data);
                }
            }

            TeamManager.addTeamDeduplicated(team);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound NBT) {
        NBTTagList teamList = new NBTTagList();
        for (Team team : TeamManager.TEAMS) {
            if (team.getMembers().isEmpty()) continue;

            NBTTagCompound teamTag = new NBTTagCompound();
            teamTag.setString("TeamName", team.getTeamName());

            // Owners
            NBTTagList ownersList = new NBTTagList();
            for (UUID owner : team.getOwners()) {
                ownersList.appendTag(new NBTTagString(owner.toString()));
            }
            teamTag.setTag("Owners", ownersList);

            // Officers
            NBTTagList officersList = new NBTTagList();
            for (UUID officer : team.getOfficers()) {
                officersList.appendTag(new NBTTagString(officer.toString()));
            }
            teamTag.setTag("Officers", officersList);

            // Members
            NBTTagList membersList = new NBTTagList();
            for (UUID member : team.getMembers()) {
                membersList.appendTag(new NBTTagString(member.toString()));
            }
            teamTag.setTag("Members", membersList);

            // Team Data
            NBTTagCompound dataTag = new NBTTagCompound();
            for (Map.Entry<String, ITeamData> entry : team.getAllDataEntries()) {
                NBTTagCompound entryTag = new NBTTagCompound();
                entry.getValue().writeToNBT(entryTag);
                dataTag.setTag(entry.getKey(), entryTag);
            }

            teamList.appendTag(teamTag);
        }
        NBT.setTag("TeamList", teamList);
    }
}
