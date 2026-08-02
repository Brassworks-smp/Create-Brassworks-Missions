package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class UseTotemMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:use_totem";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onLivingUseTotem(LivingUseTotemEvent event, ActiveMission mission) {
        if (event.getEntity() instanceof Player player) {
            if (player.level().isClientSide || mission.isComplete()) {
                return false;
            }

            if (event.getTotem().is(Items.TOTEM_OF_UNDYING)) {
                mission.incrementProgress(1);
                return true;
            }
        }
        return false;
    }
}
