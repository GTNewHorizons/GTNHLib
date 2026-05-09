package com.gtnewhorizon.gtnhlib.teams;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.WorldEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;
import com.gtnewhorizon.gtnhlib.util.NBTJson;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

@EventBusSubscriber
public class TeamDataSaver {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SAVE_VER = 1;
    public static TeamDataSaver INSTANCE;

    private File saveDir;

    private TeamDataSaver(File saveDir) {
        this.saveDir = saveDir;
        saveDir.mkdir();
        loadFromFiles();
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
            TeamManager.clear();
            INSTANCE = new TeamDataSaver(new File(event.world.getSaveHandler().getWorldDirectory(), "gtnhteams"));
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
            if (INSTANCE != null) {
                INSTANCE.saveToFiles();
                TeamManager.clear();
                INSTANCE = null;
            }
        }
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
            if (INSTANCE != null) {
                INSTANCE.saveToFiles();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            Team team = TeamManager.getOrCreateTeam(player.getCommandSenderName(), player.getUniqueID());
            TeamNetwork.sendPlayerAllTeamData(player, team);
        }
    }

    private void loadFromFiles() {
        TeamManager.clear();
        if (!saveDir.exists()) {
            GTNHLib.error("Unable to load all teams, directory does not exist: " + saveDir);
            return;
        }
        File[] files = saveDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                    Team team = loadFromNBT((NBTTagCompound) NBTJson.toNbt(obj));
                    TeamManager.addTeamDeduplicated(team);
                } catch (Exception e) {
                    GTNHLib.LOG.error("Unable to load team {}", file.getName(), e);
                }
            }
        }
    }

    private Team loadFromNBT(NBTTagCompound teamTag) {
        String teamName = teamTag.getString("TeamName");
        UUID uuid = UUID.fromString(teamTag.getString("UUID"));

        Team team = new Team(teamName, uuid);

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
            try {
                ITeamData data = TeamDataRegistry.construct(key);
                if (data != null && teamData.hasKey(key)) {
                    data.readFromNBT(teamData.getCompoundTag(key));
                }
                team.putData(key, data);
            } catch (Exception ex) {
                GTNHLib.LOG.error("Error while loading TeamData {} for team {}", key, uuid, ex);
            }
        }
        return team;
    }

    private void saveToFiles() {
        if (!saveDir.exists() && !saveDir.mkdir()) {
            GTNHLib.error("Unable to save all teams, unable to create directory: " + saveDir);
            return;
        }

        for (Team team : TeamManager.TEAMS) {
            TeamSaveStatus status = team.getStatus();
            if (status == TeamSaveStatus.CLEAN) continue;
            String fileName = team.getTeamId().toString() + ".json";
            if (status == TeamSaveStatus.DIRTY) {
                try {
                    File saveFile = new File(saveDir, fileName);
                    NBTTagCompound tag = writeToNBT(team);
                    String json = GSON.toJson(NBTJson.toJsonObject(tag));
                    Files.write(saveFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
                    team.markClean();
                } catch (Exception e) {
                    GTNHLib.LOG.error("Unable to save team {} ({})", team.getTeamId(), team.getTeamName(), e);
                }
            }
        }

        for (UUID removedTeam : TeamManager.REMOVED_TEAMS) {
            String fileName = removedTeam + ".json";
            File deleteFile = new File(saveDir, fileName);
            try {
                Files.deleteIfExists(deleteFile.toPath());
            } catch (IOException e) {
                GTNHLib.LOG.error("Unable to delete team {}", fileName, e);
            }
        }
        TeamManager.REMOVED_TEAMS.clear();
    }

    private NBTTagCompound writeToNBT(Team team) {
        NBTTagCompound teamTag = new NBTTagCompound();
        teamTag.setInteger("Version", SAVE_VER);
        teamTag.setString("TeamName", team.getTeamName());
        teamTag.setString("UUID", team.getTeamId().toString());

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
            try {
                NBTTagCompound entryTag = new NBTTagCompound();
                entry.getValue().writeToNBT(entryTag);
                dataTag.setTag(entry.getKey(), entryTag);
            } catch (Exception ex) {
                GTNHLib.LOG.error(
                        "Error while saving TeamData {} for team {}",
                        entry.getKey(),
                        team.getTeamId().toString(),
                        ex);
            }
        }
        teamTag.setTag("TeamData", dataTag);

        return teamTag;
    }
}
