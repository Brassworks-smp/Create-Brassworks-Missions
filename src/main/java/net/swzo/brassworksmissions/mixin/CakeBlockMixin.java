package net.swzo.brassworksmissions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.swzo.brassworksmissions.missions.types.vanilla.EatCakeSliceMissionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public class CakeBlockMixin {

    @Inject(method = "eat", at = @At("HEAD"))
    private static void onEat(LevelAccessor level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.canEat(false)) {
            EatCakeSliceMissionType.handleSliceEaten(serverPlayer);
        }
    }
}
