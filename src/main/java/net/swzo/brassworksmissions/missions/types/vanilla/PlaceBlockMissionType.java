package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class PlaceBlockMissionType implements IMissionType {
    public static final String ID = "brassworksmissions:place_block";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onBlockPlaced(BlockEvent.EntityPlaceEvent event, ActiveMission mission) {
        if (!(event.getEntity() instanceof ServerPlayer) || mission.isComplete()) {
            return false;
        }

        ItemStack placedStack = new ItemStack(event.getPlacedBlock().getBlock());
        ItemStack requiredStack = mission.getRequirementItemStack();
        if (!requiredStack.isEmpty() && ItemStack.isSameItem(placedStack, requiredStack)) {
            mission.incrementProgress(1);
            return true;
        }
        return false;
    }
}

