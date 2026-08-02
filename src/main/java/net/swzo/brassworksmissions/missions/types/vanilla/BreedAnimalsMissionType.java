package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class BreedAnimalsMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:breed_animals";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onBabyEntitySpawn(BabyEntitySpawnEvent event, ActiveMission mission) {
        Player player = event.getCausedByPlayer();
        if (player != null) {
            if (player.level().isClientSide || mission.isComplete()) {
                return false;
            }

            ItemStack requiredStack = mission.getRequirementItemStack();
            if (requiredStack.getItem() instanceof SpawnEggItem spawnEgg) {
                EntityType<?> requiredBabyType = spawnEgg.getType(requiredStack);
                if (event.getChild().getType() == requiredBabyType) {
                    mission.incrementProgress(1);
                    return true;
                }
            }
        }
        return false;
    }
}


