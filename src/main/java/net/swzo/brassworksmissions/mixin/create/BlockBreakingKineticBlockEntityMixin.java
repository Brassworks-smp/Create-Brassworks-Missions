package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.swzo.brassworksmissions.missions.types.create.DrillMissionType;
import net.swzo.brassworksmissions.missions.types.create.SawMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockBreakingKineticBlockEntity.class, remap = false)
public abstract class BlockBreakingKineticBlockEntityMixin {

    @Inject(method = "onBlockBroken", at = @At("HEAD"))
    public void onBlockBroken(BlockState stateToBreak, CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;

        if (self.getLevel() == null || self.getLevel().isClientSide()) {
            return;
        }

        ItemStack result = stateToBreak.getBlock().asItem().getDefaultInstance();
        if (result.isEmpty()) {
            return;
        }

        if (self instanceof SawBlockEntity) {
            MixinUtils.handleMixinMissionItem(self, SawMissionType.class, result);
        } else if (self instanceof DrillBlockEntity) {
            MixinUtils.handleMixinMissionItem(self, DrillMissionType.class, result);
        }
    }
}
