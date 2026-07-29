package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class SmeltItemMissionType implements IMissionType {
    public static final String ID = "brassworksmissions:smelt_item";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onItemSmelted(PlayerEvent.ItemSmeltedEvent event, ActiveMission mission) {
        if (!(event.getEntity() instanceof ServerPlayer) || mission.isComplete()) {
            return false;
        }

        ItemStack smelted = event.getSmelting();
        ItemStack requiredStack = mission.getRequirementItemStack();
        if (!requiredStack.isEmpty() && ItemStack.isSameItem(smelted, requiredStack)) {
            mission.incrementProgress(smelted.getCount());
            return true;
        }
        return false;
    }
}
