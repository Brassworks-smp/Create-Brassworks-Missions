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
        bankUnclaimedRewards(player, data);
        data.resizeToConfiguredSlotCount();
        int slotCount = data.getSlotCount();

        data.clear();
        RandomSource random = player.getRandom();
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        playerVariables.SelectedMission = 0;
        playerVariables.reRollAmount = 1;
        Set<String> assignedMissionIds = new HashSet<>();

        int attempts = 0;
        int maxAttempts = slotCount * 20;
        int slot = 0;

        while (slot < slotCount && attempts < maxAttempts) {
            Mission potentialMission = BrassworksmissionsMod.getMissionManager().getWeightedRandomMission(random);

            if (potentialMission != null && assignedMissionIds.add(potentialMission.getId())) {
                ActiveMission activeMission = potentialMission.createInstance(random);
                if (activeMission != null) {
                    data.setMission(slot, activeMission);
                    slot++;
                } else {
                    assignedMissionIds.remove(potentialMission.getId());
                }
            }
            attempts++;
        }

        if (slot < slotCount) {
            BrassworksmissionsMod.LOGGER.warn("Could not find enough unique missions for player {}. Filled {} of {} slots.",
                    player.getName().getString(), slot, slotCount);
        }

        playerVariables.trackedMissions.clear();
        playerVariables.syncPlayerVariables(player);
    }

    @Nullable
    private static Mission findUniqueMissionForSlot(Player player, int excludedSlot, RandomSource random) {
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
            Mission potentialMission = BrassworksmissionsMod.getMissionManager().getWeightedRandomMission(random);
            if (potentialMission != null && !existingMissionIds.contains(potentialMission.getId())) {
                return potentialMission;
            }
            attempts++;
        }

        BrassworksmissionsMod.LOGGER.warn("Could not find a unique mission to reroll for player {} after {} attempts. A non-unique mission may be assigned.", player.getName().getString(), maxAttempts);
        return BrassworksmissionsMod.getMissionManager().getWeightedRandomMission(random);
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

        Item rerollitem = BrassworksmissionsMod.getRewardManager().getRewardItem().getItem();
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

    private static int bankUnclaimedRewards(ServerPlayer player, PlayerMissionData data) {
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(
                BrassworksmissionsModVariables.PLAYER_VARIABLES
        );
        int banked = 0;
        for (ActiveMission mission : data.getMissions()) {
            if (mission == null || !mission.isComplete() || mission.isClaimed()) {
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
