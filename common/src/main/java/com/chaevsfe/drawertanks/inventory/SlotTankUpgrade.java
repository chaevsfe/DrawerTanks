package com.chaevsfe.drawertanks.inventory;

import com.jaquadro.minecraft.storagedrawers.core.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotTankUpgrade extends Slot
{
    public SlotTankUpgrade (Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace (@NotNull ItemStack stack) {
        if (stack.isEmpty())
            return false;

        if (container instanceof InventoryTankUpgrade inventory)
            return getItem().isEmpty() ? inventory.canAddUpgrade(stack) : inventory.canSwapUpgrade(getContainerSlot(), stack);

        return false;
    }

    @Override
    public boolean mayPickup (@NotNull Player player) {
        if (!(container instanceof InventoryTankUpgrade inventory))
            return true;

        if (!inventory.canRemoveUpgrade(getContainerSlot()))
            return false;

        if (!player.isCreative()) {
            ItemStack stack = getItem();
            return stack.getItem() != ModItems.CREATIVE_STORAGE_UPGRADE.get()
                && stack.getItem() != ModItems.CREATIVE_VENDING_UPGRADE.get();
        }

        return true;
    }
}
