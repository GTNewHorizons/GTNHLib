package com.gtnewhorizon.gtnhlib.teams;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.world.WorldEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.GTNHLib;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

@EventBusSubscriber
public class TeamDataSaver {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SAVE_VER = 1;
    public static TeamDataSaver INSTANCE;

    private File saveDir;
    private boolean dirty = false;

    private TeamDataSaver(File saveDir) {
        this.saveDir = saveDir;
        saveDir.mkdir();
        loadFromFiles();
    }

    public static void markForSaving() {
        if (INSTANCE != null) {
            // if at the time of calling, the instance is null, then there is no need to mark it dirty anyway
            INSTANCE.dirty = true;
        }
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
                if (INSTANCE.dirty) {
                    INSTANCE.saveToFiles();
                }
                TeamManager.clear();
                INSTANCE = null;
            }
        }
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
            if (INSTANCE != null && INSTANCE.dirty) {
                INSTANCE.dirty = false;
                INSTANCE.saveToFiles();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP player) {
            TeamManager.getOrCreateTeam(player.getCommandSenderName(), player.getUniqueID());
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
                try (FileReader fileReader = new FileReader(file)) {
                    JsonObject obj = GSON.fromJson(fileReader, JsonObject.class);
                    TeamManager.addTeamDeduplicated(jsonToTeam(obj));
                } catch (Exception e) {
                    GTNHLib.LOG.error("Unable to load team {}", file.getName(), e);
                }
            }
        }

    }

    private Team jsonToTeam(JsonObject obj) {
        String teamName = obj.get("TeamName").getAsString();
        UUID uuid = UUID.fromString(obj.get("UUID").getAsString());
        Team team = new Team(teamName, uuid);

        // Owners
        for (JsonElement elem : obj.getAsJsonArray("Owners")) {
            team.addOwner(UUID.fromString(elem.getAsString()));
        }

        // Officers
        for (JsonElement elem : obj.getAsJsonArray("Officers")) {
            team.addOfficer(UUID.fromString(elem.getAsString()));
        }

        // Members
        for (JsonElement elem : obj.getAsJsonArray("Members")) {
            team.addMember(UUID.fromString(elem.getAsString()));
        }

        JsonObject teamData = obj.getAsJsonObject("TeamData");

        for (String key : TeamDataRegistry.getRegisteredKeys()) {
            try {
                if (teamData.has(key)) {
                    ITeamData data = TeamDataRegistry.construct(key);
                    if (data != null) {
                        data.load(teamData.getAsJsonObject(key));
                        team.putData(key, data);
                    }
                }
            } catch (Exception ex) {
                GTNHLib.LOG.error("Error while loading TeamData {} for team {}", key, uuid, ex);
            }

        }

        return team;
    }

    private void saveToFiles() {
        if (!saveDir.exists()) {
            GTNHLib.error("Unable to save all teams, directory does not exist: " + saveDir);
            return;
        }
        for (Team team : TeamManager.TEAMS) {
            try {
                File saveFile = new File(saveDir, team.getTeamId().toString() + ".json");
                JsonObject obj = teamToJson(team);
                String json = GSON.toJson(obj);
                Files.write(saveFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                GTNHLib.LOG.error("Unable to save team {}", team.getTeamId(), e);
            }
        }
    }

    private JsonObject teamToJson(Team team) {
        JsonObject obj = new JsonObject();
        obj.addProperty("Version", SAVE_VER);
        obj.addProperty("TeamName", team.getTeamName());
        obj.addProperty("UUID", team.getTeamId().toString());

        JsonArray owners = new JsonArray();
        for (UUID owner : team.getOwners()) {
            owners.add(new JsonPrimitive(owner.toString()));
        }
        obj.add("Owners", owners);

        JsonArray officers = new JsonArray();
        for (UUID officer : team.getOfficers()) {
            officers.add(new JsonPrimitive(officer.toString()));
        }
        obj.add("Officers", officers);

        JsonArray members = new JsonArray();
        for (UUID member : team.getMembers()) {
            members.add(new JsonPrimitive(member.toString()));
        }
        obj.add("Members", members);

        JsonObject teamData = new JsonObject();
        for (Entry<String, ITeamData> entry : team.getAllDataEntries()) {
            try {
                JsonObject data = new JsonObject();
                entry.getValue().save(data);
                teamData.add(entry.getKey(), data);
            } catch (Exception ex) {
                GTNHLib.LOG.error(
                        "Error while saving TeamData {} for team {}",
                        entry.getKey(),
                        team.getTeamId().toString(),
                        ex);
            }
        }
        obj.add("TeamData", teamData);

        return obj;
    }
}
