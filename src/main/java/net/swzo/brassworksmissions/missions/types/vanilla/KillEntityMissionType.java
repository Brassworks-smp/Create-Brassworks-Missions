package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class KillEntityMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:kill_entity";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onLivingDeath(LivingDeathEvent event, ActiveMission mission) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide || mission.isComplete()) {
                return false;
            }

            LivingEntity killedEntity = event.getEntity();
            ItemStack requiredStack = mission.getRequirementItemStack();

            if (requiredStack.getItem() instanceof SpawnEggItem spawnEgg) {
                EntityType<?> requiredEntityType = spawnEgg.getType(requiredStack);
                if (killedEntity.getType() == requiredEntityType) {
                    mission.incrementProgress(1);
                    return true;
                }
            }
        }
        return false;
    }
}


