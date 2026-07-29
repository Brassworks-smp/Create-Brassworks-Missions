package net.swzo.brassworksmissions.missions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class ActiveMission {
    private final String missionId;
    private final String missionType;
    private final String title;
    private final ItemStack requirementItemStack;
    private final ItemStack iconItemStack;
    private final int requiredAmount;
    private final String requirementType;
    private final ItemStack rewardItemStack;
    private int progress;
    private boolean claimed;

    /**
     * Legacy constructor retained for source and save compatibility. It treats
     * the template id and event type as the same value.
     */
    public ActiveMission(
            String missionType,
            String title,
            ItemStack requirementItemStack,
            int requiredAmount,
            String requirementType,
            ItemStack rewardItemStack
    ) {
        this(
                missionType,
                missionType,
                title,
                requirementItemStack,
                requirementItemStack,
                requiredAmount,
                requirementType,
                rewardItemStack
        );
    }

    public ActiveMission(
            String missionId,
            String missionType,
            String title,
            ItemStack requirementItemStack,
            int requiredAmount,
            String requirementType,
            ItemStack rewardItemStack
    ) {
        this(
                missionId,
                missionType,
                title,
                requirementItemStack,
                requirementItemStack,
                requiredAmount,
                requirementType,
                rewardItemStack
        );
    }

    public ActiveMission(
            String missionId,
            String missionType,
            String title,
            ItemStack requirementItemStack,
            ItemStack iconItemStack,
            int requiredAmount,
            String requirementType,
            ItemStack rewardItemStack
    ) {
        this.missionId = missionId;
        this.missionType = missionType;
        this.title = title;
        this.requirementItemStack = requirementItemStack;
        this.iconItemStack = iconItemStack;
        this.requiredAmount = requiredAmount;
        this.requirementType = requirementType;
        this.rewardItemStack = rewardItemStack;
        this.progress = 0;
        this.claimed = false;
    }

    public String getTitle() {
        return title;
    }

    public int getProgress() {
        return progress;
    }

    public ItemStack getRequirementItemStack() {
        return requirementItemStack.copy();
    }

    public ItemStack getIconItemStack() {
        return iconItemStack.copy();
    }

    public ItemStack getRewardItemStack() {
        return rewardItemStack.copy();
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public String getMissionId() {
        return missionId;
    }

    public String getMissionType() {
        return missionType;
    }

    public boolean isComplete() {
        return progress >= requiredAmount;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public String getRequirementType() {
        return requirementType;
    }

    public boolean isRequirementType(String type) {
        return this.requirementType != null && this.requirementType.equalsIgnoreCase(type);
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public void incrementProgress(int amount) {
        if (!isComplete() && amount > 0) {
            this.progress = Math.min(this.progress + amount, requiredAmount);
        }
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(progress, requiredAmount));
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("missionId", missionId);
        nbt.putString("missionType", missionType);
        nbt.putString("title", title);
        if (requirementItemStack != null && !requirementItemStack.isEmpty()) {
            nbt.put("requirementItem", requirementItemStack.save(provider));
        }
        if (iconItemStack != null && !iconItemStack.isEmpty()) {
            nbt.put("iconItem", iconItemStack.save(provider));
        }
        nbt.putInt("requiredAmount", requiredAmount);
        if (requirementType != null) {
            nbt.putString("requirementType", requirementType);
        }
        nbt.put("rewardItem", rewardItemStack.save(provider));
        nbt.putInt("progress", progress);
        nbt.putBoolean("claimed", claimed);
        return nbt;
    }

    public static ActiveMission deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        String missionType = nbt.getString("missionType");
        String missionId = nbt.contains("missionId", Tag.TAG_STRING) ? nbt.getString("missionId") : missionType;
        ItemStack requirementItem = ItemStack.parse(
                provider,
                nbt.getCompound("requirementItem")
        ).orElse(ItemStack.EMPTY);
        ItemStack iconItem = nbt.contains("iconItem", Tag.TAG_COMPOUND)
                ? ItemStack.parse(provider, nbt.getCompound("iconItem")).orElse(requirementItem.copy())
                : requirementItem.copy();
        ActiveMission mission = new ActiveMission(
                missionId,
                missionType,
                nbt.getString("title"),
                requirementItem,
                iconItem,
                nbt.getInt("requiredAmount"),
                getRequirementTypeFromNBT(nbt),
                ItemStack.parse(provider, nbt.getCompound("rewardItem")).orElse(ItemStack.EMPTY)
        );

        mission.setProgress(nbt.getInt("progress"));
        mission.claimed = nbt.getBoolean("claimed");
        return mission;
    }

    private static String getRequirementTypeFromNBT(CompoundTag nbt) {
        if (nbt.contains("requirementType", Tag.TAG_STRING)) {
            return nbt.getString("requirementType");
        }
        if (nbt.contains("isItemRequirement")) {
            return nbt.getBoolean("isItemRequirement") ? "item" : "block";
        }
        return "";
    }
}
