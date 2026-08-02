package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.swzo.brassworksmissions.missions.types.create.CompactMissionType;
import net.swzo.brassworksmissions.missions.types.create.MixMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {

    @Shadow
    protected Recipe<?> currentRecipe;

    @Inject(method = "applyBasinRecipe", at = @At("RETURN"))
    private void onApplyBasinRecipe(CallbackInfo ci) {
        if (this.currentRecipe != null) {
            BasinOperatingBlockEntity self = (BasinOperatingBlockEntity) (Object) this;

            if (currentRecipe instanceof MixingRecipe || (currentRecipe instanceof CraftingRecipe && !(currentRecipe instanceof ShapedRecipe))) {
                MixinUtils.handleMixinMissionItem( self, MixMissionType.class, currentRecipe.getResultItem(RegistryAccess.EMPTY));
            }

            if (currentRecipe instanceof CompactingRecipe || currentRecipe instanceof ShapedRecipe) {
                MixinUtils.handleMixinMissionItem( self, CompactMissionType.class, currentRecipe.getResultItem(RegistryAccess.EMPTY));
            }
        }
    }
}
