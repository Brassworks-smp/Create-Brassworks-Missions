package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class ConsumeItemMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:consume_item";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onItemConsumed(LivingEntityUseItemEvent.Finish event, ActiveMission mission) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || mission.isComplete()) {
            return false;
        }

        ItemStack consumedStack = event.getItem();
        ItemStack requiredStack = mission.getRequirementItemStack();

        if (requiredStack != null && !requiredStack.isEmpty() &&
                ItemStack.isSameItem(consumedStack, requiredStack)) {
            mission.incrementProgress(1);
            return true;
        }
        return false;
    }
}

