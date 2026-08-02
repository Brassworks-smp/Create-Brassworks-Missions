package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.world.item.ItemStack;
import net.swzo.brassworksmissions.missions.types.create.CutMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SawBlockEntity.class, remap = false)
public class SawBlockEntityMixin {

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/saw/SawBlockEntity;applyRecipe()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onApplyCuttingRecipe(CallbackInfo ci) {
        SawBlockEntity self = (SawBlockEntity) (Object) this;
        ProcessingInventory inventory = self.inventory;

        if (self.getLevel() == null || self.getLevel().isClientSide() || inventory == null) {
            return;
        }

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack result = inventory.getStackInSlot(i);
            if (!result.isEmpty()) {
                MixinUtils.handleMixinMissionItem(self, CutMissionType.class, result);
            }
        }
    }
}


