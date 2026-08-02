package net.swzo.brassworksmissions.util;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.LevelAccessor;
import net.swzo.brassworksmissions.world.inventory.UiMenu;
import org.jetbrains.annotations.NotNull;

public class UiUtils {
	public static void openUi(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ServerPlayer serverPlayer) {
			BlockPos blockPos = BlockPos.containing(x, y, z);
			serverPlayer.openMenu(new MenuProvider() {
				@Override
				public @NotNull Component getDisplayName() {
					return Component.literal("Ui");
				}

				@Override
				public boolean shouldTriggerClientSideContainerClosingOnOpen() {
					return false;
				}

				@Override
				public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
					return new UiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(blockPos));
				}
			}, blockPos);
		}
	}

    public static void closeUi(Entity entity) {
        if (entity == null)
            return;
        if (entity instanceof Player player)
            player.closeContainer();
    }
}
