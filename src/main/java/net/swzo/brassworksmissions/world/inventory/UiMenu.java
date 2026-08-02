package net.swzo.brassworksmissions.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.swzo.brassworksmissions.init.BrassworksmissionsModMenus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UiMenu extends AbstractContainerMenu implements BrassworksmissionsModMenus.MenuAccessor {
    public final Map<String, Object> menuState = new HashMap<>() {
        @Override
        public Object put(String key, Object value) {
            if (!this.containsKey(key) && this.size() >= 4)
                return null;
            return super.put(key, value);
        }
    };
    public final Player entity;
    public int x, y, z;

    public UiMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(BrassworksmissionsModMenus.UI.get(), id);
        this.entity = inv.player;
        if (extraData != null) {
            BlockPos pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public Map<Integer, Slot> getSlots() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getMenuState() {
        return menuState;
    }
}
