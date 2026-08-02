package net.swzo.brassworksmissions.missions.types.vanilla;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;

public class BreakBlockMissionType implements IMissionType {
    private static final String ID = "brassworksmissions:break_block";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean onBlockBroken(BlockEvent.BreakEvent event, ActiveMission mission) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide || mission.isComplete()) {
            return false;
        }

        BlockState brokenBlock = event.getState();
        ItemStack requiredStack = mission.getRequirementItemStack();

        if (!requiredStack.isEmpty() &&
                ItemStack.isSameItem(new ItemStack(brokenBlock.getBlock()), requiredStack)) {
            mission.incrementProgress(1);
            return true;
        }
        return false;
    }
}
