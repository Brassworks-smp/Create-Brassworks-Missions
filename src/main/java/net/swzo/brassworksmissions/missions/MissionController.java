package net.swzo.brassworksmissions.missions;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.event.MissionEvent;
import net.swzo.brassworksmissions.init.CustomStats;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;

import javax.annotation.Nullable;
import java.util.*;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

public class MissionController {

    private static final Map<Integer, ResourceLocation> ADVANCEMENT_THRESHOLDS = Map.of(
            10, ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "missions_10"),
            50, ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "missions_50"),
            100, ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "missions_100"),
            250, ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "missions_250"),
            500, ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "missions_500"),
            1000, ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "missions_1000")
    );



    public static PlayerMissionData getMissionData(Player player) {
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        if (playerVariables.missionData == null) {
            playerVariables.missionData = new PlayerMissionData();
        }
        return playerVariables.missionData;
    }

    @Nullable
    public static ActiveMission getMissionInSlot(Player player, int slot) {
        PlayerMissionData data = getMissionData(player);
        if (slot < 0 || slot >= data.getSlotCount()) {
            return null;
        }

        return data.getMission(slot);
    }

    public static void reassignMissions(ServerPlayer player) {
        PlayerMissionData data = getMissionData(player);
        bankUnclaimedRewards(player, data, null);
        data.resizeToConfiguredSlotCount();
        data.clear();
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        playerVariables.SelectedMission = 0;
        playerVariables.reRollAmount = 1;
        assignEmptySlots(player, data);
        playerVariables.trackedMissions.clear();
        playerVariables.syncPlayerVariables(player);
    }

    public static void reassignMissions(ServerPlayer player, int category) {
        PlayerMissionData data = getMissionData(player);
        int oldCategoryOneSlots = data.getAppliedCategoryOneSlotCount();
        int oldCategoryTwoSlots = data.getAppliedCategoryTwoSlotCount();
        bankUnclaimedRewards(player, data, category);
        data.resizeCategoryToConfiguredSlotCount(category);
        for (int slot = 0; slot < data.getSlotCount(); slot++) {
            if (data.getCategoryForSlot(slot) == category) {
                data.setMission(slot, null);
            }
        }
        assignEmptySlots(player, data);
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        playerVariables.SelectedMission = 0;
        playerVariables.reRollAmount = 1;
        remapTrackedMissions(
                playerVariables.trackedMissions,
                category,
                oldCategoryOneSlots,
                oldCategoryTwoSlots,
                data.getAppliedCategoryOneSlotCount(),
                data.getAppliedCategoryTwoSlotCount()
        );
        playerVariables.syncPlayerVariables(player);
    }

    public static boolean initializeMissingCategoryTwo(ServerPlayer player) {
        PlayerMissionData data = getMissionData(player);
        if (!data.initializeConfiguredCategoryTwo()) {
            return false;
        }
        assignEmptySlots(player, data, 2);
        player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(player);
        return true;
    }

    private static void remapTrackedMissions(
            Collection<Integer> trackedMissions,
            int reassignedCategory,
            int oldCategoryOneSlots,
            int oldCategoryTwoSlots,
            int newCategoryOneSlots,
            int newCategoryTwoSlots
    ) {
        Set<Integer> remapped = new HashSet<>();
        for (int slot : trackedMissions) {
            if (reassignedCategory == 1) {
                int categoryTwoSlot = slot - oldCategoryOneSlots;
                if (categoryTwoSlot >= 0 && categoryTwoSlot < Math.min(oldCategoryTwoSlots, newCategoryTwoSlots)) {
                    remapped.add(newCategoryOneSlots + categoryTwoSlot);
                }
            } else if (slot >= 0 && slot < Math.min(oldCategoryOneSlots, newCategoryOneSlots)) {
                remapped.add(slot);
            }
        }
        trackedMissions.clear();
        trackedMissions.addAll(remapped);
    }

    private static void assignEmptySlots(ServerPlayer player, PlayerMissionData data) {
        assignEmptySlots(player, data, null);
    }

    private static void assignEmptySlots(
            ServerPlayer player,
            PlayerMissionData data,
            @Nullable Integer selectedCategory
    ) {
        RandomSource random = player.getRandom();
        Set<String> assignedMissionIds = new HashSet<>();
        for (ActiveMission mission : data.getMissions()) {
            if (mission != null) {
                assignedMissionIds.add(mission.getMissionId());
            }
        }

        for (int slot = 0; slot < data.getSlotCount(); slot++) {
            if (data.getMission(slot) != null) {
                continue;
            }
            int category = data.getCategoryForSlot(slot);
            if (selectedCategory != null && category != selectedCategory) {
                continue;
            }
            int attempts = 0;
            while (attempts++ < 20) {
                Mission potentialMission = BrassworksmissionsMod.getMissionManager()
                        .getWeightedRandomMission(player, random, category);
                if (potentialMission != null && assignedMissionIds.add(potentialMission.getId())) {
                    ActiveMission activeMission = potentialMission.createInstance(random);
                    if (activeMission != null) {
                        data.setMission(slot, activeMission);
                        break;
                    }
                    assignedMissionIds.remove(potentialMission.getId());
                }
            }
            if (data.getMission(slot) == null) {
                BrassworksmissionsMod.LOGGER.warn(
                        "Could not fill {} mission slot {} for player {}.",
                        "category " + category,
                        slot + 1,
                        player.getName().getString()
                );
            }
        }
    }

    @Nullable
    private static Mission findUniqueMissionForSlot(ServerPlayer player, int excludedSlot, RandomSource random) {
        PlayerMissionData data = getMissionData(player);
        Set<String> existingMissionIds = new HashSet<>();
        for (int i = 0; i < data.getSlotCount(); i++) {
            if (i == excludedSlot) continue;
            ActiveMission mission = data.getMission(i);
            if (mission != null) {
                existingMissionIds.add(mission.getMissionId());
            }
        }

        int attempts = 0;
        int maxAttempts = 20;

        while (attempts < maxAttempts) {
            int category = data.getCategoryForSlot(excludedSlot);
            Mission potentialMission = BrassworksmissionsMod.getMissionManager()
                    .getWeightedRandomMission(player, random, category);
            if (potentialMission != null && !existingMissionIds.contains(potentialMission.getId())) {
                return potentialMission;
            }
            attempts++;
        }

        BrassworksmissionsMod.LOGGER.warn("Could not find a unique mission to reroll for player {} after {} attempts. A non-unique mission may be assigned.", player.getName().getString(), maxAttempts);
        return BrassworksmissionsMod.getMissionManager().getWeightedRandomMission(
                player,
                random,
                data.getCategoryForSlot(excludedSlot)
        );
    }

    public static void rerollMission(ServerPlayer player, int slot) {
        PlayerMissionData data = getMissionData(player);
        ActiveMission currentMission = data.getMission(slot);
        if (slot < 0 || slot >= data.getSlotCount() || currentMission == null) {
            player.sendSystemMessage(Component.literal("Invalid or empty mission slot.").withStyle(ChatFormatting.RED));
            return;
        }
        if (currentMission.isComplete()) {
            player.sendSystemMessage(Component.translatable("gui.brassworksmissions.error.completed_mission")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        RandomSource random = player.getRandom();
        Mission missionTemplate = findUniqueMissionForSlot(player, slot, random);
        ActiveMission newMission = missionTemplate == null ? null : missionTemplate.createInstance(random);
        if (newMission == null) {
            player.sendSystemMessage(Component.literal("No valid replacement mission is available.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        int rerollCost = 0;
        if (playerVariables.reRollAmount > 2) {
            rerollCost = (playerVariables.reRollAmount - 2) * 2;
        }

        Item rerollitem = BrassworksmissionsMod.getRewardItem().getItem();
        ItemStack rerollstack = new ItemStack(rerollitem);

        if (rerollCost > 32) {
            player.sendSystemMessage(Component.translatable(
                    "gui.brassworksmissions.error.cost_cap_reached",
                    rerollstack.getHoverName().copy().append(Component.translatable("gui.brassworksmissions.ui.plural_format"))
            ).withStyle(ChatFormatting.RED));
            return;
        }
        if (rerollCost > 0) {
            int playerOwned = player.getInventory().countItem(rerollitem);
            if (playerOwned < rerollCost) {
                player.sendSystemMessage(Component.translatable(
                        "gui.brassworksmissions.error.not_enough",
                        rerollCost,
                        rerollstack.getHoverName().copy().append(Component.translatable("gui.brassworksmissions.ui.plural_format"))
                ).withStyle(ChatFormatting.RED));
                return;
            }
            player.getInventory().clearOrCountMatchingItems(
                    stack -> stack.is(rerollitem), rerollCost, player.inventoryMenu.getCraftSlots()
            );
            player.inventoryMenu.broadcastChanges();
            player.sendSystemMessage(Component.translatable(
                    "gui.brassworksmissions.success.rerolled",
                    rerollCost,
                    rerollstack.getHoverName().copy().append(Component.translatable("gui.brassworksmissions.ui.plural_format"))
            ).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "gui.brassworksmissions.success.rerolled_free"
            ).withStyle(ChatFormatting.GREEN));
        }

        playerVariables.reRollAmount++;
        data.setMission(slot, newMission);
        playerVariables.syncPlayerVariables(player);
        EVENT_BUS.post(new MissionEvent.Rerolled(player, newMission, slot, false));
    }

    public static boolean forceRerollMission(ServerPlayer player, int slot) {
        PlayerMissionData data = getMissionData(player);
        if (slot < 0 || slot >= data.getSlotCount()) {
            return false;
        }

        RandomSource random = player.getRandom();
        Mission missionTemplate = findUniqueMissionForSlot(player, slot, random);
        if (missionTemplate != null) {
            ActiveMission newMission = missionTemplate.createInstance(random);
            if (newMission == null) {
                return false;
            }
            data.setMission(slot, newMission);
            player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(player);

            EVENT_BUS.post(new MissionEvent.Rerolled(player, newMission, slot, true));
            return true;
        }
        return false;
    }

    public static boolean isMissionCompleted(Player player, int slot) {
        ActiveMission mission = getMissionInSlot(player, slot);
        return mission != null && mission.isComplete();
    }

    public static void resetRerollCost(ServerPlayer player) {
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        if (playerVariables.reRollAmount > 1) {
            playerVariables.reRollAmount = 1;
            playerVariables.syncPlayerVariables(player);
        }
    }

    public static boolean forceCompleteMission(ServerPlayer player, int slot) {
        ActiveMission mission = getMissionInSlot(player, slot);
        if (mission == null || mission.isComplete()) {
            return false;
        }

        int targetAmount = mission.getRequiredAmount();
        mission.setProgress(targetAmount);

        player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(player);

        EVENT_BUS.post(new MissionEvent.Completed(player, mission, slot));
        return true;
    }

    private static boolean claimRewardInternal(ServerPlayer player, int slot) {
        ActiveMission mission = getMissionInSlot(player, slot);
        if (mission == null || !mission.isComplete() || mission.isClaimed()) {
            return false;
        }
        mission.setClaimed(true);
        deliverReward(player, mission.getRewardItemStack());
        player.awardStat(CustomStats.MISSIONS_COMPLETED.get());
        checkAndGrantAdvancements(player);
        EVENT_BUS.post(new MissionEvent.RewardClaimed(player, mission, slot));
        return true;
    }

    private static void deliverReward(ServerPlayer player, ItemStack reward) {
        ItemStack rewardStack = reward.copy();
        player.getInventory().add(rewardStack);
        if (!rewardStack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    rewardStack
            );
            itemEntity.setNoPickUpDelay();
            player.level().addFreshEntity(itemEntity);
        }
    }

    private static int bankUnclaimedRewards(
            ServerPlayer player,
            PlayerMissionData data,
            @Nullable Integer category
    ) {
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(
                BrassworksmissionsModVariables.PLAYER_VARIABLES
        );
        int banked = 0;
        ActiveMission[] missions = data.getMissions();
        for (int slot = 0; slot < missions.length; slot++) {
            ActiveMission mission = missions[slot];
            if (mission == null || !mission.isComplete() || mission.isClaimed()) {
                continue;
            }
            if (category != null && data.getCategoryForSlot(slot) != category) {
                continue;
            }
            playerVariables.storePendingReward(mission.getRewardItemStack());
            player.awardStat(CustomStats.MISSIONS_COMPLETED.get());
            banked++;
        }
        if (banked > 0) {
            checkAndGrantAdvancements(player);
            BrassworksmissionsMod.LOGGER.info(
                    "Stored {} unclaimed mission rewards for player {} before reassignment.",
                    banked,
                    player.getName().getString()
            );
        }
        return banked;
    }

    public static boolean claimReward(ServerPlayer player, int slot) {
        boolean success = claimRewardInternal(player, slot);
        if (success) {
            player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(player);
        }
        return success;
    }

    public static boolean claimAllRewards(ServerPlayer player) {
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(
                BrassworksmissionsModVariables.PLAYER_VARIABLES
        );
        List<ItemStack> pendingRewards = playerVariables.takePendingRewards();
        boolean anyClaimed = !pendingRewards.isEmpty();
        pendingRewards.forEach(reward -> deliverReward(player, reward));

        PlayerMissionData data = getMissionData(player);
        for (int i = 0; i < data.getSlotCount(); i++) {
            if (claimRewardInternal(player, i)) {
                anyClaimed = true;
            }
        }

        if (anyClaimed) {
            playerVariables.syncPlayerVariables(player);
        }

        return anyClaimed;
    }

    public static int getTotalClaimableItemCount(Player player) {
        PlayerMissionData data = getMissionData(player);
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(
                BrassworksmissionsModVariables.PLAYER_VARIABLES
        );
        int count = playerVariables.getPendingRewardItemCount();
        for (int i = 0; i < data.getSlotCount(); i++) {
            ActiveMission mission = data.getMission(i);
            if (mission != null && mission.isComplete() && !mission.isClaimed()) {
                count += mission.getRewardItemStack().getCount();
            }
        }
        return count;
    }

    private static void checkAndGrantAdvancements(ServerPlayer player) {
        int completedMissions = player.getStats().getValue(Stats.CUSTOM.get(CustomStats.MISSIONS_COMPLETED.get()));

        for (Map.Entry<Integer, ResourceLocation> entry : ADVANCEMENT_THRESHOLDS.entrySet()) {
            int requiredCount = entry.getKey();
            ResourceLocation advancementId = entry.getValue();

            if (completedMissions >= requiredCount) {
                AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
                if (advancement != null) {
                    for (String criterion : advancement.value().criteria().keySet()) {
                        player.getAdvancements().award(advancement, criterion);
                    }
                }
            }
        }
    }
}
