package net.swzo.brassworksmissions.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.swzo.brassworksmissions.missions.*;
import net.swzo.brassworksmissions.missions.types.create.CreateMissionType;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;
import net.swzo.brassworksmissions.event.MissionEvent;

import javax.annotation.Nullable;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
public class MixinUtils {

    public static <T extends CreateMissionType<?>> void handleMixinMissionItem(BlockEntity be, Class<T> missionClass, ItemStack output) {
        if (be == null || be.getLevel() == null || be.getLevel().isClientSide() || output == null || output.isEmpty()) {
            return;
        }

        Level level = be.getLevel();
        BlockPos worldPosition = be.getBlockPos();
        Player player = getClosestPlayer(level, worldPosition);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerMissionData missionData = MissionController.getMissionData(serverPlayer);
        if (missionData == null) {
            return;
        }

        boolean needsSync = false;

        for (int i = 0; i < missionData.getSlotCount(); i++) {
            ActiveMission mission = missionData.getMission(i);

            if (mission == null || mission.isComplete()) {
                continue;
            }

            IMissionType type = MissionRegistry.getMissionType(mission.getMissionType());

            if (type != null && missionClass.isInstance(type)) {
                T missionType = missionClass.cast(type);
                if (missionType.check(output, mission)) {
                    needsSync = true;
                    if (mission.isComplete()) {
                        EVENT_BUS.post(new MissionEvent.Completed(serverPlayer, mission, i));
                    }
                }
            }
        }

        if (needsSync) {
            serverPlayer.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(serverPlayer);
        }
    }

    public static <T extends CreateMissionType<?>> void handlePlayerMissionIncrement(Player player, Class<T> missionClass, ItemStack itemStack) {
        if (player == null || player.level() == null || player.level().isClientSide() || itemStack == null || itemStack.isEmpty()) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerMissionData missionData = MissionController.getMissionData(serverPlayer);
        if (missionData == null) {
            return;
        }

        boolean needsSync = false;

        for (int i = 0; i < missionData.getSlotCount(); i++) {
            ActiveMission mission = missionData.getMission(i);

            if (mission == null || mission.isComplete()) {
                continue;
            }

            IMissionType type = MissionRegistry.getMissionType(mission.getMissionType());

            if (type != null && missionClass.isInstance(type)) {
                T missionType = missionClass.cast(type);
                if (missionType.check(itemStack, mission)) {
                    needsSync = true;
                    if (mission.isComplete()) {
                        EVENT_BUS.post(new MissionEvent.Completed(serverPlayer, mission, i));
                    }
                }
            }
        }

        if (needsSync) {
            serverPlayer.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(serverPlayer);
        }
    }

    @Nullable
    public static Player getClosestPlayer(Level level, BlockPos worldPosition) {
        if (level == null || worldPosition == null) return null;
        return level.getNearestPlayer(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                128,
                false);
    }
}
