package net.swzo.brassworksmissions.mixin.create;

import com.simibubi.create.content.kinetics.saw.TreeCutter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.swzo.brassworksmissions.missions.types.create.SawMissionType;
import net.swzo.brassworksmissions.util.MixinUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

@Mixin(value = TreeCutter.Tree.class, remap = false)
public class TreeMixin {

    @Shadow @Final private List<BlockPos> leaves;
    @Shadow @Final private List<BlockPos> logs;

    @Inject(method = "destroyBlocks", at = @At("HEAD"))
    public void onDestroyBlocks(Level world, ItemStack toDamage, Player playerEntity, BiConsumer<BlockPos, ItemStack> drop, CallbackInfo ci) {
        if (world.isClientSide() || logs.isEmpty()) {
            return;
        }

        final Player closestPlayer = MixinUtils.getClosestPlayer(world, logs.get(0));
        if (!(closestPlayer instanceof ServerPlayer)) {
            return;
        }

        Set<BlockPos> allBlocks = new HashSet<>(logs);
        allBlocks.addAll(leaves);

        allBlocks.forEach(pos -> {
            ItemStack blockStack = new ItemStack(world.getBlockState(pos).getBlock().asItem());
            if (!blockStack.isEmpty()) {
                MixinUtils.handlePlayerMissionIncrement(closestPlayer, SawMissionType.class, blockStack);
            }
        });
    }
}

