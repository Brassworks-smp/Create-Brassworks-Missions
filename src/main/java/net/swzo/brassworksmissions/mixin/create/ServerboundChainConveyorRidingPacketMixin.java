package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ServerboundChainConveyorRidingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.swzo.brassworksmissions.missions.types.create.TravelByChainConveyorMissionType;
import net.swzo.brassworksmissions.util.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerboundChainConveyorRidingPacket.class, remap = false)
public class ServerboundChainConveyorRidingPacketMixin {

    @Shadow
    private boolean stop;

    @Inject(method = "applySettings*", at = @At("HEAD"))
    private void onApplySettings(ServerPlayer sender, ChainConveyorBlockEntity be, CallbackInfo ci) {
        if (sender != null && !this.stop) {
            DistanceManager.track(sender, TravelByChainConveyorMissionType.ID, 20);
        }
    }
}


