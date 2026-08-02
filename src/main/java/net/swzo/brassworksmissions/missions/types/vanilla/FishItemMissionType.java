package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class FishItemMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:fish_item";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onItemFished(ItemFishedEvent event, ActiveMission mission) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (mission.isComplete()) {
                return false;
            }

            ItemStack requiredStack = mission.getRequirementItemStack();
            if (requiredStack.isEmpty()) {
                return false;
            }

            for (ItemStack fishedStack : event.getDrops()) {
                if (ItemStack.isSameItemSameComponents(fishedStack, requiredStack)) {
                    mission.incrementProgress(fishedStack.getCount());
                    return true;
                }
            }
        }
        return false;
    }
}

