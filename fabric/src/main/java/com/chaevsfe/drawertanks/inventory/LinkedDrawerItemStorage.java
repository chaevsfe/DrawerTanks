package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.LinkedItemChannels;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

public class LinkedDrawerItemStorage extends SnapshotParticipant<LinkedDrawerItemStorage.State> implements SingleSlotStorage<ItemVariant>
{
    public record State(ItemStack prototype, long count) { }

    private final LinkedItemChannels.Pool pool;

    private LinkedDrawerItemStorage (LinkedItemChannels.Pool pool) {
        this.pool = pool;
    }

    // cached on the pool, not the block entity: every drawer on a channel must share one participant,
    // and the pool holds no reference back into the level
    public static LinkedDrawerItemStorage of (BlockEntityLinkedDrawer drawer) {
        LinkedItemChannels.Pool pool = drawer.pool();
        if (pool == null)
            return null;

        if (pool.platformHandler instanceof LinkedDrawerItemStorage storage)
            return storage;

        LinkedDrawerItemStorage storage = new LinkedDrawerItemStorage(pool);
        pool.platformHandler = storage;
        return storage;
    }

    @Override
    public long insert (ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0)
            return 0;

        ItemStack incoming = resource.toStack(1);
        if (!pool.isEmpty() && !ItemStack.isSameItemSameComponents(pool.prototype, incoming))
            return 0;

        long space = Math.max(0, LinkedItemChannels.Pool.capacityFor(incoming) - pool.count);
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

        if (pool.isEmpty() || !ItemStack.isSameItemSameComponents(pool.prototype, resource.toStack(1)))
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
        return pool.prototype.isEmpty();
    }

    @Override
    public ItemVariant getResource () {
        return pool.prototype.isEmpty() ? ItemVariant.blank() : ItemVariant.of(pool.prototype);
    }

    @Override
    public long getAmount () {
        return pool.count;
    }

    @Override
    public long getCapacity () {
        return pool.capacity();
    }

    @Override
    protected State createSnapshot () {
        return new State(pool.prototype.copy(), pool.count);
    }

    @Override
    protected void readSnapshot (State snapshot) {
        pool.set(snapshot.prototype(), snapshot.count());
    }

    @Override
    protected void onFinalCommit () {
        pool.changed();
    }
}
