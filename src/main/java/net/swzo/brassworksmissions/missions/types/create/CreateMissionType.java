package net.swzo.brassworksmissions.missions.types.create;

import net.minecraft.world.item.ItemStack;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public abstract class CreateMissionType<T extends CreateMissionType<T>> implements IMissionType {

    public boolean check(ItemStack output, ActiveMission mission) {
        if (mission.isComplete()) {
            return false;
        }

        ItemStack requiredStack = mission.getRequirementItemStack();

        if (requiredStack != null && !requiredStack.isEmpty() &&
                ItemStack.isSameItem(output, requiredStack)) {

            mission.incrementProgress(output.getCount());
            return true;
        }
        return false;
    }
}
