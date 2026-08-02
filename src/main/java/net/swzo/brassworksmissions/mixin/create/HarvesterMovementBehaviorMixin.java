package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.swzo.brassworksmissions.missions.types.create.HarvesterMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HarvesterMovementBehaviour.class, remap = false)
public class HarvesterMovementBehaviorMixin {
    @Inject(
            method = "cutCrop",
            at = @At("HEAD")
    )
    private void onCutCrop(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<BlockState> cir) {

        if (level.isClientSide()) return;
        ItemStack harvestedCrop = state.getBlock().asItem().getDefaultInstance();

        if (harvestedCrop.isEmpty()) return;
        Player player = MixinUtils.getClosestPlayer(level, pos);
        MixinUtils.handlePlayerMissionIncrement(player, HarvesterMissionType.class, harvestedCrop);
    }
}
