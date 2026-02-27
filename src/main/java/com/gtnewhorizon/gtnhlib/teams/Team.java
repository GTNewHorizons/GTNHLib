package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.Getter;

public class Team {

    @Getter
    private String teamName;
    private final ObjectList<UUID> owners = new ObjectArrayList<>();
    private final ObjectList<UUID> members = new ObjectArrayList<>();
    private final Map<String, ITeamData> teamData = new HashMap<>();

    public Team(String teamName) {
        this.teamName = teamName;
    }

    public boolean renameTeam(String newName) {
        if (TeamManager.isTeamNameValid(newName)) {
            this.teamName = newName;
            TeamWorldSavedData.markForSaving();
            return true;
        }
        return false;
    }

    public boolean isTeamMember(UUID player) {
        return members.contains(player);
    }

    public void addMember(UUID uuid) {
        if (!members.contains(uuid)) members.add(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        owners.remove(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public List<UUID> getMembers() {
        return ObjectLists.unmodifiable(members);
    }

    public boolean isTeamOwner(UUID player) {
        return owners.contains(player);
    }

    public void addOwner(UUID uuid) {
        if (!owners.contains(uuid)) owners.add(uuid);
        // owners are also always members
        if (!members.contains(uuid)) members.add(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public List<UUID> getOwners() {
        return owners;
    }

    public void initializeData(String... keys) {
        for (String key : keys) {
            if (!teamData.containsKey(key)) {
                ITeamData data = TeamDataRegistry.construct(key);
                if (data != null) teamData.put(key, data);
            }
        }
    }

    void putData(String key, ITeamData data) {
        teamData.put(key, data);
    }

    public ITeamData getData(String key) {
        return teamData.get(key);
    }

    public List<ITeamData> getAllData() {
        return new ArrayList<>(teamData.values());
    }

    public Set<Map.Entry<String, ITeamData>> getAllDataEntries() {
        return teamData.entrySet();
    }
}
