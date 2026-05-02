package com.gtnewhorizon.gtnhlib.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import lombok.Getter;

public class Team {

    @Getter
    private String teamName;
    @Getter
    private final UUID teamId;
    private final ObjectSet<UUID> owners = new ObjectOpenHashSet<>();
    private final ObjectSet<UUID> officers = new ObjectOpenHashSet<>();
    private final ObjectSet<UUID> members = new ObjectOpenHashSet<>();
    private final Map<String, ITeamData> teamData = new HashMap<>();

    Team(String teamName, UUID teamId) {
        this.teamName = teamName;
        this.teamId = teamId;
    }

    public boolean renameTeam(String newName) {
        if (TeamManager.isTeamNameValid(newName)) {
            this.teamName = newName;
            TeamWorldSavedData.markForSaving();
            return true;
        }
        return false;
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public boolean isOfficer(UUID player) {
        return officers.contains(player);
    }

    public boolean isOwner(UUID player) {
        return owners.contains(player);
    }

    public void addMember(UUID uuid) {
        if (members.add(uuid)) TeamWorldSavedData.markForSaving();
    }

    public void addOfficer(UUID uuid) {
        if (!officers.add(uuid)) return;
        // officers are also always members
        members.add(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public void addOwner(UUID uuid) {
        if (!owners.add(uuid)) return;
        // owners are also always members and officers
        officers.add(uuid);
        members.add(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        officers.remove(uuid);
        owners.remove(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public void removeOfficer(UUID uuid) {
        owners.remove(uuid);
        officers.remove(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public void removeOwner(UUID uuid) {
        owners.remove(uuid);
        TeamWorldSavedData.markForSaving();
    }

    public Set<UUID> getMembers() {
        return ObjectSets.unmodifiable(members);
    }

    public Set<UUID> getOfficers() {
        return ObjectSets.unmodifiable(officers);
    }

    public Set<UUID> getOwners() {
        return ObjectSets.unmodifiable(owners);
    }

    void initializeData(String... keys) {
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
