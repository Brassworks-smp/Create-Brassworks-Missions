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
    public static final int MAX_MISSION_SLOTS = 64;
    private ActiveMission[] missions;
    private int categoryOneSlotCount;
    private int categoryTwoSlotCount;

    public PlayerMissionData() {
        categoryOneSlotCount = getCategoryOneSlotCount();
        categoryTwoSlotCount = getCategoryTwoSlotCount();
        missions = new ActiveMission[categoryOneSlotCount + categoryTwoSlotCount];
    }

    public static int getConfiguredSlotCount() {
        int categoryOne = getCategoryOneSlotCount();
        int categoryTwo = getCategoryTwoSlotCount();
        return Math.min(MAX_MISSION_SLOTS, categoryOne + categoryTwo);
    }

    public static int getCategoryOneSlotCount() {
        return Math.max(1, Math.min(MAX_MISSION_SLOTS, Config.SERVER.MISSION_SLOT_COUNT.get()));
    }

    public static int getCategoryTwoSlotCount() {
        if (!Config.SERVER.CATEGORY_TWO_ENABLED.get()) {
            return 0;
        }
        int remaining = MAX_MISSION_SLOTS - getCategoryOneSlotCount();
        return Math.max(0, Math.min(remaining, Config.SERVER.CATEGORY_TWO_SLOT_COUNT.get()));
    }

    public int getSlotCount() {
        return missions.length;
    }

    public int getAppliedCategoryOneSlotCount() {
        return categoryOneSlotCount;
    }

    public int getAppliedCategoryTwoSlotCount() {
        return categoryTwoSlotCount;
    }

    public int getCategoryForSlot(int slot) {
        return slot >= categoryOneSlotCount ? 2 : 1;
    }

    public boolean resizeToConfiguredSlotCount() {
        return resize(getCategoryOneSlotCount(), getCategoryTwoSlotCount());
    }

    public boolean resizeCategoryToConfiguredSlotCount(int category) {
        int categoryOne = category == 1 ? getCategoryOneSlotCount() : categoryOneSlotCount;
        int categoryTwo = category == 2 ? getCategoryTwoSlotCount() : categoryTwoSlotCount;
        return resize(categoryOne, categoryTwo);
    }

    public boolean initializeConfiguredCategoryTwo() {
        int configuredCategoryTwo = getCategoryTwoSlotCount();
        if (configuredCategoryTwo == 0 || categoryTwoSlotCount > 0) {
            return false;
        }
        int configuredCategoryOne = getCategoryOneSlotCount();
        if (missions.length == configuredCategoryOne + configuredCategoryTwo) {
            boolean compatible = true;
            for (int slot = configuredCategoryOne; slot < missions.length; slot++) {
                ActiveMission mission = missions[slot];
                if (mission != null && mission.getCategory() != 2) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                categoryOneSlotCount = configuredCategoryOne;
                categoryTwoSlotCount = configuredCategoryTwo;
                return true;
            }
        }
        return resize(categoryOneSlotCount, configuredCategoryTwo);
    }

    private boolean resize(int categoryOne, int categoryTwo) {
        categoryOne = Math.max(1, Math.min(MAX_MISSION_SLOTS, categoryOne));
        categoryTwo = Math.max(0, Math.min(MAX_MISSION_SLOTS - categoryOne, categoryTwo));
        if (categoryOneSlotCount == categoryOne && categoryTwoSlotCount == categoryTwo) {
            return false;
        }
        ActiveMission[] resized = new ActiveMission[categoryOne + categoryTwo];
        System.arraycopy(missions, 0, resized, 0, Math.min(categoryOneSlotCount, categoryOne));
        System.arraycopy(
                missions,
                categoryOneSlotCount,
                resized,
                categoryOne,
                Math.min(categoryTwoSlotCount, categoryTwo)
        );
        missions = resized;
        categoryOneSlotCount = categoryOne;
        categoryTwoSlotCount = categoryTwo;
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
        nbt.putInt("CategoryOneSlotCount", categoryOneSlotCount);
        nbt.putInt("CategoryTwoSlotCount", categoryTwoSlotCount);
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
        serializedSlots = Math.min(serializedSlots, MAX_MISSION_SLOTS);
        if (nbt.contains("CategoryOneSlotCount", Tag.TAG_INT)) {
            categoryOneSlotCount = Math.max(1, Math.min(serializedSlots, nbt.getInt("CategoryOneSlotCount")));
        } else {
            categoryOneSlotCount = inferCategoryOneSlotCount(missionList, serializedSlots);
        }
        categoryTwoSlotCount = serializedSlots - categoryOneSlotCount;
        missions = new ActiveMission[serializedSlots];
        for (int i = 0; i < Math.min(missionList.size(), missions.length); i++) {
            CompoundTag missionTag = missionList.getCompound(i);
            if (missionTag.contains("MissionData", Tag.TAG_COMPOUND)) {
                missions[i] = ActiveMission.deserializeNBT(provider, missionTag.getCompound("MissionData"));
            } else {
                missions[i] = null;
            }
        }
    }

    private int inferCategoryOneSlotCount(ListTag missionList, int serializedSlots) {
        for (int i = 0; i < Math.min(missionList.size(), serializedSlots); i++) {
            CompoundTag missionTag = missionList.getCompound(i);
            if (missionTag.contains("MissionData", Tag.TAG_COMPOUND)
                    && missionTag.getCompound("MissionData").getInt("category") == 2) {
                return Math.max(1, i);
            }
        }
        if (getCategoryTwoSlotCount() > 0 && serializedSlots == getConfiguredSlotCount()) {
            return getCategoryOneSlotCount();
        }
        return serializedSlots;
    }
}
