package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.world.item.ItemStack;
import net.swzo.brassworksmissions.missions.types.create.CrushMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
public class CrushingWheelControllerBlockEntityMixin {

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/crusher/CrushingWheelControllerBlockEntity;applyRecipe()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onTickAfterRecipe(CallbackInfo ci) {

        CrushingWheelControllerBlockEntity self = (CrushingWheelControllerBlockEntity) (Object) this;
        ProcessingInventory inventory = self.inventory;

        if (self.getLevel() == null || self.getLevel().isClientSide() || inventory == null) {
            return;
        }

        for (int i = 1; i < inventory.getSlots(); i++) {
            ItemStack result = inventory.getStackInSlot(i);
            if (!result.isEmpty()) {

                MixinUtils.handleMixinMissionItem(self, CrushMissionType.class, result);
            }
        }
    }
}
