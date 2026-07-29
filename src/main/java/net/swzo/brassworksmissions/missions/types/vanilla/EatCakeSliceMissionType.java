package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;
import net.swzo.brassworksmissions.event.MissionEvent;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

public class EatCakeSliceMissionType implements IMissionType {
    public static final String ID = "brassworksmissions:eat_cake_slice";

    @Override
    public String getId() {
        return ID;
    }

    public static void handleSliceEaten(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean needsSync = false;
        var missions = serverPlayer.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).missionData.getMissions();

        for (int slot = 0; slot < missions.length; slot++) {
            ActiveMission mission = missions[slot];
            if (mission != null && mission.getMissionType().equals(ID) && !mission.isComplete()) {

                if (mission.getRequirementItemStack().is(Items.CAKE)) {
                    mission.incrementProgress(1);
                    needsSync = true;
                    if (mission.isComplete()) {
                        EVENT_BUS.post(new MissionEvent.Completed(serverPlayer, mission, slot));
                    }
                }
            }
        }

        if (needsSync) {
            serverPlayer.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(serverPlayer);
        }
    }
}
