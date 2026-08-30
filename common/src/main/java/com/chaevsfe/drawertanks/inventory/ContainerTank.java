package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.core.ModContainers;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgrade;
import com.texelsaurus.minecraft.chameleon.inventory.content.PositionContent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContainerTank extends AbstractContainerMenu
{
    private static final int InventoryX = 8;
    private static final int InventoryY = 117;
    private static final int HotbarY = 175;
    private static final int UpgradeX = 26;
    private static final int UpgradeY = 86;
    private static final int DisplayX = 80;
    private static final int DisplayY = 38;

    @Nullable
    private final UpgradeHost tank;
    private final InventoryTankUpgrade upgradeInventory;
    private final List<Slot> upgradeSlots = new ArrayList<>();

    @Nullable
    private Slot displaySlot;

    public ContainerTank (int windowId, Inventory playerInv, Optional<PositionContent> content) {
        this(windowId, playerInv, resolveHost(playerInv, content));
    }

    // the menu serves both tanks and linked drawers, so resolve the block entity then narrow it
    @Nullable
    private static UpgradeHost resolveHost (Inventory playerInv, Optional<PositionContent> content) {
        net.minecraft.world.level.block.entity.BlockEntity entity = PositionContent.getOrNull(
            content, playerInv.player.level(), net.minecraft.world.level.block.entity.BlockEntity.class);
        return entity instanceof UpgradeHost host ? host : null;
    }

    public ContainerTank (int windowId, Inventory playerInventory, @Nullable UpgradeHost tank) {
        super(ModContainers.TANK_CONTAINER.get(), windowId);

        this.tank = tank;
        upgradeInventory = new InventoryTankUpgrade(tank);

        for (int i = 0; i < BlockEntityTank.UPGRADE_SLOTS; i++)
            upgradeSlots.add(addSlot(new SlotTankUpgrade(upgradeInventory, i, UpgradeX + i * 18, UpgradeY)));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(playerInventory, j + i * 9 + 9, InventoryX + j * 18, InventoryY + i * 18));
        }

        for (int i = 0; i < 9; i++)
            addSlot(new Slot(playerInventory, i, InventoryX + i * 18, HotbarY));

        // a linked drawer shows what its channel holds where a tank has its gauge; added last so the
        // quick-move ranges above keep their indices, and inert so nothing can be taken from it
        if (tank instanceof com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer drawer) {
            displaySlot = addSlot(new Slot(new DisplayContainer(drawer), 0, DisplayX, DisplayY)
            {
                @Override
                public boolean mayPlace (@NotNull ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup (@NotNull Player player) {
                    return false;
                }
            });
        }
    }

    @Nullable
    public Slot getDisplaySlot () {
        return displaySlot;
    }

    // read-only view of the channel's stored item, so vanilla renders it for us
    private record DisplayContainer(com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer drawer) implements net.minecraft.world.Container
    {
        @Override
        public int getContainerSize () {
            return 1;
        }

        @Override
        public boolean isEmpty () {
            return drawer.displayItem().isEmpty();
        }

        @Override
        @NotNull
        public ItemStack getItem (int slot) {
            ItemStack stored = drawer.displayItem();
            return stored.isEmpty() ? ItemStack.EMPTY : stored.copyWithCount(1);
        }

        @Override
        @NotNull
        public ItemStack removeItem (int slot, int count) {
            return ItemStack.EMPTY;
        }

        @Override
        @NotNull
        public ItemStack removeItemNoUpdate (int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem (int slot, @NotNull ItemStack stack) { }

        @Override
        public void setChanged () { }

        @Override
        public boolean stillValid (@NotNull Player player) {
            return true;
        }

        @Override
        public void clearContent () { }
    }

    @Nullable
    public UpgradeHost getTank () {
        return tank;
    }

    public List<Slot> getUpgradeSlots () {
        return upgradeSlots;
    }

    @Override
    public boolean stillValid (@NotNull Player player) {
        return upgradeInventory.stillValid(player);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack (@NotNull Player player, int slotIndex) {
        int upgradeEnd = upgradeSlots.size();
        int inventoryStart = upgradeEnd;
        int hotbarStart = inventoryStart + 27;
        int hotbarEnd = hotbarStart + 9;

        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack result = slotStack.copy();

        if (slotIndex < upgradeEnd) {
            if (!moveItemStackTo(slotStack, inventoryStart, hotbarEnd, true))
                return ItemStack.EMPTY;
        } else if (slotStack.getItem() instanceof ItemUpgrade
            && moveItemStackTo(slotStack.copyWithCount(1), 0, upgradeEnd, false)) {
            slotStack.shrink(1);
            if (slotStack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
            slot.onTake(player, slotStack);
            return ItemStack.EMPTY;
        } else if (slotIndex < hotbarStart) {
            if (!moveItemStackTo(slotStack, hotbarStart, hotbarEnd, false))
                return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(slotStack, inventoryStart, hotbarStart, false))
                return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        if (slotStack.getCount() == result.getCount())
            return ItemStack.EMPTY;

        slot.onTake(player, slotStack);
        return result;
    }
}
