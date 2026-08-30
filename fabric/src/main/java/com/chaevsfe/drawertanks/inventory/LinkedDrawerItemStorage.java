package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.LinkedItemChannels;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class LinkedDrawerItemStorage extends SnapshotParticipant<LinkedDrawerItemStorage.State> implements SingleSlotStorage<ItemVariant>
{
    public record State(ItemStack prototype, long count) { }

    private static final Map<BlockEntityLinkedDrawer, LinkedDrawerItemStorage> WRAPPERS =
        new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private final BlockEntityLinkedDrawer drawer;

    private LinkedDrawerItemStorage (BlockEntityLinkedDrawer drawer) {
        this.drawer = drawer;
    }

    public static LinkedDrawerItemStorage of (BlockEntityLinkedDrawer drawer) {
        return WRAPPERS.computeIfAbsent(drawer, LinkedDrawerItemStorage::new);
    }

    private LinkedItemChannels.Pool pool () {
        return drawer.pool();
    }

    @Override
    public long insert (ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0)
            return 0;

        LinkedItemChannels.Pool pool = pool();
        if (pool == null)
            return 0;

        ItemStack incoming = resource.toStack(1);
        if (!pool.isEmpty() && !ItemStack.isSameItemSameComponents(pool.prototype, incoming))
            return 0;

        long space = Math.max(0, drawer.capacityItems() - pool.count);
        long accepted = Math.min(maxAmount, space);
        if (accepted <= 0)
            return 0;

        updateSnapshots(transaction);
        if (pool.isEmpty())
            pool.set(incoming, accepted);
        else
            pool.count += accepted;
        return accepted;
    }

    @Override
    public long extract (ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0)
            return 0;

        LinkedItemChannels.Pool pool = pool();
        if (pool == null || pool.isEmpty() || !ItemStack.isSameItemSameComponents(pool.prototype, resource.toStack(1)))
            return 0;

        long extracted = Math.min(maxAmount, pool.count);
        if (extracted <= 0)
            return 0;

        updateSnapshots(transaction);
        pool.count -= extracted;
        if (pool.count <= 0)
            pool.set(ItemStack.EMPTY, 0);
        return extracted;
    }

    @Override
    public boolean isResourceBlank () {
        LinkedItemChannels.Pool pool = pool();
        return pool == null || pool.prototype.isEmpty();
    }

    @Override
    public ItemVariant getResource () {
        LinkedItemChannels.Pool pool = pool();
        return pool == null || pool.prototype.isEmpty() ? ItemVariant.blank() : ItemVariant.of(pool.prototype);
    }

    @Override
    public long getAmount () {
        LinkedItemChannels.Pool pool = pool();
        return pool == null ? 0 : pool.count;
    }

    @Override
    public long getCapacity () {
        return drawer.capacityItems();
    }

    @Override
    protected State createSnapshot () {
        LinkedItemChannels.Pool pool = pool();
        return pool == null ? new State(ItemStack.EMPTY, 0) : new State(pool.prototype.copy(), pool.count);
    }

    @Override
    protected void readSnapshot (State snapshot) {
        LinkedItemChannels.Pool pool = pool();
        if (pool != null)
            pool.set(snapshot.prototype(), snapshot.count());
    }

    @Override
    protected void onFinalCommit () {
        drawer.onPoolChanged();
    }
}
