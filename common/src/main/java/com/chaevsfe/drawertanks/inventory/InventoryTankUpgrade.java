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
    private final BlockEntityTank tank;

    public InventoryTankUpgrade (@Nullable BlockEntityTank tank) {
        this.tank = tank;
    }

    @Nullable
    public BlockEntityTank getTank () {
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
            if (!tank.upgrades().getUpgrade(i).isEmpty())
                return false;
        }
        return true;
    }

    @Override
    @NotNull
    public ItemStack getItem (int slot) {
        return tank == null ? ItemStack.EMPTY : tank.upgrades().getUpgrade(slot);
    }

    @Override
    @NotNull
    public ItemStack removeItem (int slot, int count) {
        ItemStack stack = getItem(slot);
        if (count > 0 && tank != null)
            tank.upgrades().setUpgrade(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    @NotNull
    public ItemStack removeItemNoUpdate (int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem (int slot, @NotNull ItemStack item) {
        if (tank != null)
            tank.upgrades().setUpgrade(slot, item);
    }

    @Override
    public int getMaxStackSize () {
        return 1;
    }

    @Override
    public void setChanged () {
        if (tank != null)
            tank.setChanged();
    }

    @Override
    public boolean stillValid (@NotNull Player player) {
        if (tank == null || tank.getLevel() == null)
            return false;
        if (tank.getLevel().getBlockEntity(tank.getBlockPos()) != tank)
            return false;
        return player.distanceToSqr(Vec3.atCenterOf(tank.getBlockPos())) <= 64.0;
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
        return tank != null && tank.upgrades().canAddUpgrade(item) && tank.upgradeFitsContents(item);
    }

    public boolean canRemoveUpgrade (int slot) {
        if (tank == null || getItem(slot).isEmpty())
            return false;
        return tank.tankData().getAmount() <= tank.capacityDropletsWithout(slot);
    }

    public boolean canSwapUpgrade (int slot, @NotNull ItemStack item) {
        if (item.getCount() > 1)
            return false;
        return canRemoveUpgrade(slot) && tank != null && tank.upgrades().canSwapUpgrade(slot, item);
    }

    public boolean slotIsLocked (int slot) {
        return !getItem(slot).isEmpty() && !canRemoveUpgrade(slot);
    }
}
