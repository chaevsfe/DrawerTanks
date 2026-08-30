package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InventoryTankUpgrade implements Container
{
    private static final int upgradeCapacity = BlockEntityTank.UPGRADE_SLOTS;

    @Nullable
    private final UpgradeHost tank;

    // Client side the menu owns its slots, the way a vanilla container does. Reading through to the
    // block entity would let its update packet rewrite the array the open screen is drawing from,
    // which is what made the upgrade slots flicker.
    private final ItemStack[] clientItems = new ItemStack[upgradeCapacity];

    public InventoryTankUpgrade (@Nullable UpgradeHost tank) {
        this.tank = tank;
        java.util.Arrays.fill(clientItems, ItemStack.EMPTY);
    }

    private boolean clientOwned () {
        return tank != null && tank.hostLevel() != null && tank.hostLevel().isClientSide();
    }

    @Nullable
    public UpgradeHost getTank () {
        return tank;
    }

    @Override
    public int getContainerSize () {
        return upgradeCapacity;
    }

    @Override
    public boolean isEmpty () {
        if (tank == null)
            return true;

        for (int i = 0; i < upgradeCapacity; i++) {
            if (!getItem(i).isEmpty())
                return false;
        }
        return true;
    }

    @Override
    @NotNull
    public ItemStack getItem (int slot) {
        if (slot < 0 || slot >= upgradeCapacity)
            return ItemStack.EMPTY;
        if (clientOwned())
            return clientItems[slot];
        return tank == null ? ItemStack.EMPTY : tank.upgrades().getUpgrade(slot);
    }

    @Override
    @NotNull
    public ItemStack removeItem (int slot, int count) {
        ItemStack stack = getItem(slot).copy();
        if (count > 0)
            setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    @NotNull
    public ItemStack removeItemNoUpdate (int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem (int slot, @NotNull ItemStack item) {
        if (slot < 0 || slot >= upgradeCapacity)
            return;

        if (clientOwned()) {
            clientItems[slot] = item;
            return;
        }

        if (tank != null) {
            tank.upgrades().setUpgrade(slot, item);
            tank.refreshUpgradeMirror();
        }
    }

    @Override
    public int getMaxStackSize () {
        return 1;
    }

    @Override
    public void setChanged () {
        if (tank != null)
            tank.hostChanged();
    }

    @Override
    public boolean stillValid (@NotNull Player player) {
        if (tank == null || tank.hostLevel() == null)
            return false;
        if (tank.hostLevel().getBlockEntity(tank.hostPos()) != tank)
            return false;
        return player.distanceToSqr(Vec3.atCenterOf(tank.hostPos())) <= 64.0;
    }

    @Override
    public void startOpen (@NotNull ContainerUser user) { }

    @Override
    public void stopOpen (@NotNull ContainerUser user) { }

    @Override
    public boolean canPlaceItem (int slot, @NotNull ItemStack item) {
        return canAddUpgrade(item);
    }

    @Override
    public void clearContent () { }

    public boolean canAddUpgrade (@NotNull ItemStack item) {
        return tank != null && tank.acceptsUpgrades() && BlockEntityTank.upgradeApplies(item)
            && tank.upgrades().canAddUpgrade(item) && tank.upgradeFitsContents(item);
    }

    public boolean canRemoveUpgrade (int slot) {
        if (tank == null || getItem(slot).isEmpty())
            return false;
        return tank.storedAmount() <= tank.capacityWithout(slot);
    }

    public boolean canSwapUpgrade (int slot, @NotNull ItemStack item) {
        if (item.getCount() > 1 || tank == null || getItem(slot).isEmpty())
            return false;
        if (!BlockEntityTank.upgradeApplies(item))
            return false;
        if (!tank.upgrades().canSwapUpgrade(slot, item) || !tank.upgradeFitsContents(item))
            return false;
        return tank.storedAmount() <= tank.capacityWithSwap(slot, item);
    }

    public boolean slotIsLocked (int slot) {
        return !getItem(slot).isEmpty() && !canRemoveUpgrade(slot);
    }
}
