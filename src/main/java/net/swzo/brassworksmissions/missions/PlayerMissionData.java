package net.swzo.brassworksmissions.missions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.swzo.brassworksmissions.config.Config;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerMissionData {
    public static final int MAX_MISSION_SLOTS = 32;
    private ActiveMission[] missions;

    public PlayerMissionData() {
        missions = new ActiveMission[getConfiguredSlotCount()];
    }

    public static int getConfiguredSlotCount() {
        return Math.max(1, Math.min(MAX_MISSION_SLOTS, Config.SERVER.MISSION_SLOT_COUNT.get()));
    }

    public int getSlotCount() {
        return missions.length;
    }

    public boolean resizeToConfiguredSlotCount() {
        int configuredSlots = getConfiguredSlotCount();
        if (missions.length == configuredSlots) {
            return false;
        }
        missions = Arrays.copyOf(missions, configuredSlots);
        return true;
    }

    @Nullable
    public ActiveMission getMission(int slot) {
        if (slot < 0 || slot >= missions.length) {
            return null;
        }
        return missions[slot];
    }

    public void setMission(int slot, @Nullable ActiveMission mission) {
        if (slot >= 0 && slot < missions.length) {
            missions[slot] = mission;
        }
    }

    public List<ActiveMission> getActiveMissions() {
        List<ActiveMission> activeMissions = new ArrayList<>();
        for (ActiveMission mission : missions) {
            if (mission != null) {
                activeMissions.add(mission);
            }
        }
        return activeMissions;
    }

    public ActiveMission[] getMissions() {
        return Arrays.copyOf(missions, missions.length);
    }

    public void clear() {
        Arrays.fill(missions, null);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("SlotCount", missions.length);
        ListTag missionList = new ListTag();
        for (ActiveMission mission : missions) {
            CompoundTag missionTag = new CompoundTag();
            if (mission != null) {
                missionTag.put("MissionData", mission.serializeNBT(provider));
            }
            missionList.add(missionTag);
        }
        nbt.put("Missions", missionList);
        return nbt;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        ListTag missionList = nbt.getList("Missions", Tag.TAG_COMPOUND);
        int serializedSlots = nbt.contains("SlotCount", Tag.TAG_INT)
                ? nbt.getInt("SlotCount")
                : missionList.size();
        if (serializedSlots < 1) {
            serializedSlots = getConfiguredSlotCount();
        }
        missions = new ActiveMission[Math.min(serializedSlots, MAX_MISSION_SLOTS)];
        for (int i = 0; i < Math.min(missionList.size(), missions.length); i++) {
            CompoundTag missionTag = missionList.getCompound(i);
            if (missionTag.contains("MissionData", Tag.TAG_COMPOUND)) {
                missions[i] = ActiveMission.deserializeNBT(provider, missionTag.getCompound("MissionData"));
            } else {
                missions[i] = null;
            }
        }
    }
}
