package net.swzo.brassworksmissions.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.missions.MissionController;
import net.swzo.brassworksmissions.missions.PlayerMissionData;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;

@EventBusSubscriber(modid = BrassworksmissionsMod.MODID)
public class PlayerDataInitializer {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initializeMissionData(player);
        }
    }

    private static void initializeMissionData(ServerPlayer player) {
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        boolean slotCountChanged = MissionController.getMissionData(player).getSlotCount()
                != PlayerMissionData.getConfiguredSlotCount();
        if (!playerVariables.hasmissiondata || slotCountChanged) {
            MissionController.reassignMissions(player);
            playerVariables.hasmissiondata = true;
            playerVariables.syncPlayerVariables(player);
        }
    }
}

