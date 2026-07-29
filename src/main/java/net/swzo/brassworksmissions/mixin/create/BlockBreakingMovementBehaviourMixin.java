package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;
import net.swzo.brassworksmissions.missions.MissionRegistry;
import net.swzo.brassworksmissions.missions.types.create.CreateMissionType;
import net.swzo.brassworksmissions.missions.types.create.DrillMissionType;
import net.swzo.brassworksmissions.missions.types.create.SawMissionType;
import net.swzo.brassworksmissions.event.MissionEvent;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@Mixin(value = BlockBreakingMovementBehaviour.class, remap = false)
public class BlockBreakingMovementBehaviourMixin {

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    protected void destroyBlock(MovementContext context, BlockPos breakingPos, CallbackInfo ci) {
        Level level = context.world;
        if (level.isClientSide()) {
            return;
        }

        Block machineBlock = context.state.getBlock();
        Class<? extends CreateMissionType> missionClass;

        if (machineBlock instanceof DrillBlock) {
            missionClass = DrillMissionType.class;
        } else if (machineBlock instanceof SawBlock) {
            missionClass = SawMissionType.class;
        } else {
            return;
        }

        BlockState brokenState = level.getBlockState(breakingPos);
        ItemStack result = brokenState.getBlock().asItem().getDefaultInstance();
        if (result.isEmpty()) {
            return;
        }

        Player player = MixinUtils.getClosestPlayer(level, breakingPos);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean needsSync = false;
        var missions = serverPlayer.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).missionData.getMissions();

        for (int slot = 0; slot < missions.length; slot++) {
            ActiveMission mission = missions[slot];
            if (mission == null || mission.isComplete()) {
                continue;
            }
            IMissionType type = MissionRegistry.getMissionType(mission.getMissionType());

            if (missionClass.isInstance(type)) {
                CreateMissionType<?> missionType = missionClass.cast(type);
                if (missionType.check(result, mission)) {
                    needsSync = true;
                    if (mission.isComplete()) {
                        EVENT_BUS.post(new MissionEvent.Completed(serverPlayer, mission, slot));
                    }
                }
            }
        }

        if (needsSync) {
            serverPlayer.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(serverPlayer);
        }
    }
}
