package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.swzo.brassworksmissions.missions.types.create.MechanicalCraftMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public class MechanicalCrafterBlockEntityMixin {
    @Shadow protected RecipeGridHandler.GroupedItems groupedItems;

    @Inject(method = "continueIfAllPrecedingFinished", at = @At("HEAD"))
    private void onApplyMechanicalCraftingRecipe(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;

        ItemStack result =
                RecipeGridHandler.tryToApplyRecipe(self.getLevel(), groupedItems);
        if (result != null)
            MixinUtils.handleMixinMissionItem(self, MechanicalCraftMissionType.class, result);
    }
}



