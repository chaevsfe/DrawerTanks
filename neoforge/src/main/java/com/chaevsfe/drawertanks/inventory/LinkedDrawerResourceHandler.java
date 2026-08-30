package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.LinkedItemChannels;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Map;

public class LinkedDrawerResourceHandler implements ResourceHandler<ItemResource>
{
    record State(ItemStack prototype, long count) { }

    private static final Map<BlockEntityLinkedDrawer, LinkedDrawerResourceHandler> WRAPPERS =
        new com.google.common.collect.MapMaker().weakKeys().makeMap();

    private final BlockEntityLinkedDrawer drawer;
    private final SnapshotJournal<State> journal;
    private final RootCommitJournal commitJournal;

    private LinkedDrawerResourceHandler (BlockEntityLinkedDrawer drawer) {
        this.drawer = drawer;
        this.commitJournal = new RootCommitJournal(drawer::onPoolChanged);
        this.journal = new SnapshotJournal<>()
        {
            @Override
            protected State createSnapshot () {
                LinkedItemChannels.Pool pool = drawer.pool();
                return pool == null ? new State(ItemStack.EMPTY, 0) : new State(pool.prototype.copy(), pool.count);
            }

            @Override
            protected void revertToSnapshot (State snapshot) {
                LinkedItemChannels.Pool pool = drawer.pool();
                if (pool != null)
                    pool.set(snapshot.prototype(), snapshot.count());
            }
        };
    }

    public static LinkedDrawerResourceHandler of (BlockEntityLinkedDrawer drawer) {
        return WRAPPERS.computeIfAbsent(drawer, LinkedDrawerResourceHandler::new);
    }

    @Override
    public int size () {
        return 1;
    }

    @Override
    public ItemResource getResource (int index) {
        LinkedItemChannels.Pool pool = drawer.pool();
        return pool == null || pool.prototype.isEmpty() ? ItemResource.EMPTY : ItemResource.of(pool.prototype);
    }

    @Override
    public long getAmountAsLong (int index) {
        LinkedItemChannels.Pool pool = drawer.pool();
        return pool == null ? 0 : pool.count;
    }

    @Override
    public long getCapacityAsLong (int index, ItemResource resource) {
        return drawer.capacityItems();
    }

    @Override
    public boolean isValid (int index, ItemResource resource) {
        if (resource.isEmpty())
            return false;

        LinkedItemChannels.Pool pool = drawer.pool();
        return pool == null || pool.isEmpty() || ItemStack.isSameItemSameComponents(pool.prototype, resource.toStack(1));
    }

    @Override
    public int insert (int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        LinkedItemChannels.Pool pool = drawer.pool();
        if (pool == null)
            return 0;

        ItemStack incoming = resource.toStack(1);
        if (!pool.isEmpty() && !ItemStack.isSameItemSameComponents(pool.prototype, incoming))
            return 0;

        long space = Math.max(0, drawer.capacityItems() - pool.count);
        int accepted = (int) Math.min(amount, space);
        if (accepted <= 0)
            return 0;

        journal.updateSnapshots(transaction);
        commitJournal.updateSnapshots(transaction);
        if (pool.isEmpty())
            pool.set(incoming, accepted);
        else
            pool.count += accepted;
        return accepted;
    }

    @Override
    public int extract (int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        LinkedItemChannels.Pool pool = drawer.pool();
        if (pool == null || pool.isEmpty() || !ItemStack.isSameItemSameComponents(pool.prototype, resource.toStack(1)))
            return 0;

        int extracted = (int) Math.min(amount, pool.count);
        if (extracted <= 0)
            return 0;

        journal.updateSnapshots(transaction);
        commitJournal.updateSnapshots(transaction);
        pool.count -= extracted;
        if (pool.count <= 0)
            pool.set(ItemStack.EMPTY, 0);
        return extracted;
    }
}
