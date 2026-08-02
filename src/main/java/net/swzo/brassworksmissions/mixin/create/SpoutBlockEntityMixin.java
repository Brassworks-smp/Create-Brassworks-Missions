package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.swzo.brassworksmissions.missions.types.create.FillItemMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = SpoutBlockEntity.class, remap = false)
public abstract class SpoutBlockEntityMixin {

    @Inject(
            method = "whenItemHeld(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour;)Lcom/simibubi/create/content/kinetics/belt/behaviour/BeltProcessingBehaviour$ProcessingResult;",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lcom/simibubi/create/content/fluids/spout/FillingBySpout;fillItem(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onFilledItemCreated(
            TransportedItemStack transported,
            TransportedItemStackHandlerBehaviour handler,
            CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir,
            FluidStack fluid,
            int requiredAmountForItem,
            ItemStack out
    ) {
        SpoutBlockEntity self = (SpoutBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide() && out != null && !out.isEmpty()) {
            MixinUtils.handleMixinMissionItem( self, FillItemMissionType.class, out);
        }
    }
}


