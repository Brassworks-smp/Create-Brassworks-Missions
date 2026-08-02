package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class PickupItemMissionType implements IMissionType {
    public static final String ID = "brassworksmissions:pickup_item";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onItemPickedUp(ItemEntityPickupEvent.Post event, ActiveMission mission) {
        if (!(event.getPlayer() instanceof ServerPlayer) || mission.isComplete()) {
            return false;
        }

        ItemStack original = event.getOriginalStack();
        ItemStack requiredStack = mission.getRequirementItemStack();
        if (requiredStack.isEmpty() || !ItemStack.isSameItem(original, requiredStack)) {
            return false;
        }

        int pickedUp = original.getCount() - event.getCurrentStack().getCount();
        if (pickedUp > 0) {
            mission.incrementProgress(pickedUp);
            return true;
        }
        return false;
    }
}

