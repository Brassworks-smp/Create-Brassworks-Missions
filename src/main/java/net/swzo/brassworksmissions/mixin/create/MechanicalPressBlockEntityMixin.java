package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.swzo.brassworksmissions.missions.types.create.PressMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalPressBlockEntity.class, remap = false)
public class MechanicalPressBlockEntityMixin {

    @Inject(method = "onItemPressed", at = @At("RETURN"))
    public void afterItemPressed(ItemStack result, CallbackInfo ci) {
        MixinUtils.handleMixinMissionItem(
                (BlockEntity)(Object) this,
                PressMissionType.class,
                result);
    }
}


