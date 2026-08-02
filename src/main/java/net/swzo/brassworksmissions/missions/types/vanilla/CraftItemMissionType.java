package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class CraftItemMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:craft_item";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onItemCrafted(PlayerEvent.ItemCraftedEvent event, ActiveMission mission) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (mission.isComplete()) {
                return false;
            }

            ItemStack requiredStack = mission.getRequirementItemStack();
            ItemStack craftedStack = event.getCrafting();

            if (ItemStack.isSameItemSameComponents(craftedStack, requiredStack)) {
                mission.incrementProgress(craftedStack.getCount());
                return true;
            }
        }
        return false;
    }
}

