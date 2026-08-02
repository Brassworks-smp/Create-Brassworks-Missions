package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class ReachExperienceLevelMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:reach_experience_level";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onPlayerTick(PlayerTickEvent.Post event, ActiveMission mission) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level().isClientSide || mission.isComplete()) {
                return false;
            }

            int requiredLevel = mission.getRequiredAmount();
            if (player.experienceLevel >= requiredLevel) {
                mission.setProgress(mission.getRequiredAmount());
                return true;
            } else {

                mission.setProgress(player.experienceLevel);
                return true;
            }
        }
        return false;
    }
}
