package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.swzo.brassworksmissions.missions.types.create.MillMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MillstoneBlockEntity.class, remap = false)
public class MillstoneBlockEntityMixin {

    @Shadow
    public ItemStackHandler outputInv;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/millstone/MillstoneBlockEntity;process()V", shift = At.Shift.BEFORE))
    private void beforeProcess(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide()) {
            return;
        }

        for (int i = 0; i < outputInv.getSlots(); i++) {
            ItemStack result = outputInv.getStackInSlot(i);
            if (!result.isEmpty()) {
                MixinUtils.handleMixinMissionItem(self, MillMissionType.class, result);
            }
        }
    }
}


