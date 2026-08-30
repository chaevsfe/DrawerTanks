package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.block.tile.TankTarget;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class TankResourceHandler implements ResourceHandler<FluidResource>
{
    record State(Fluid fluid, DataComponentPatch components, long amount) { }

    private final TankTarget tank;
    private final SnapshotJournal<State> journal;
    private final RootCommitJournal commitJournal;

    private TankResourceHandler (TankTarget tank) {
        this.tank = tank;
        this.commitJournal = new RootCommitJournal(tank::onChanged);
        this.journal = new SnapshotJournal<>()
        {
            @Override
            protected State createSnapshot () {
                TankData data = tank.data();
                return new State(data.getFluid(), data.getComponents(), data.getAmount());
            }

            @Override
            protected void revertToSnapshot (State snapshot) {
                TankData data = tank.data();
                data.setFluid(snapshot.fluid(), snapshot.components());
                data.setAmount(snapshot.amount());
            }
        };
    }

    public static TankResourceHandler of (BlockEntityTank tank) {
        TankTarget target = tank.target();
        if (target.platformHandler instanceof TankResourceHandler handler)
            return handler;

        TankResourceHandler handler = new TankResourceHandler(target);
        target.platformHandler = handler;
        return handler;
    }

    @Override
    public int size () {
        return 1;
    }

    @Override
    public FluidResource getResource (int index) {
        TankData data = tank.data();
        return data.hasFluid() ? FluidResource.of(data.getFluid(), data.getComponents()) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong (int index) {
        return tank.data().getAmount() / BlockEntityTank.DROPLETS_PER_MB;
    }

    @Override
    public long getCapacityAsLong (int index, FluidResource resource) {
        return tank.capacity() / BlockEntityTank.DROPLETS_PER_MB;
    }

    @Override
    public boolean isValid (int index, FluidResource resource) {
        if (resource.isEmpty())
            return false;

        TankData data = tank.data();
        return !data.hasFluid() || data.matches(resource.value(), resource.getComponentsPatch());
    }

    @Override
    public int insert (int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        TankData data = tank.data();

        // sub-mB residue (possible on worlds coming from fabric) is replaceable unless the fluid is locked
        boolean residueOnly = data.hasFluid() && data.getAmount() > 0
            && data.getAmount() < BlockEntityTank.DROPLETS_PER_MB && !tank.isFluidLocked();
        if (data.hasFluid() && !residueOnly && !data.matches(resource.value(), resource.getComponentsPatch()))
            return 0;

        long capacity = tank.capacity();
        long space = Math.max(0, capacity - (residueOnly ? 0 : data.getAmount()));
        long spaceMb = (space + BlockEntityTank.DROPLETS_PER_MB - 1) / BlockEntityTank.DROPLETS_PER_MB;
        int accepted = (int) Math.min(amount, spaceMb);

        if (accepted > 0) {
            journal.updateSnapshots(transaction);
            commitJournal.updateSnapshots(transaction);
            if (residueOnly)
                data.clear();
            data.setFluid(resource.value(), resource.getComponentsPatch());
            data.setAmount(Math.min(capacity, data.getAmount() + accepted * BlockEntityTank.DROPLETS_PER_MB));
        }

        if (tank.isVoid() && data.hasFluid() && data.matches(resource.value(), resource.getComponentsPatch()))
            return amount;

        return accepted;
    }

    @Override
    public int extract (int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        TankData data = tank.data();
        if (!data.hasFluid() || !data.matches(resource.value(), resource.getComponentsPatch()))
            return 0;

        if (tank.isUnlimitedVending())
            return amount;

        long availableMb = data.getAmount() / BlockEntityTank.DROPLETS_PER_MB;
        int extracted = (int) Math.min(amount, availableMb);

        if (extracted > 0) {
            journal.updateSnapshots(transaction);
            commitJournal.updateSnapshots(transaction);
            data.setAmount(data.getAmount() - extracted * BlockEntityTank.DROPLETS_PER_MB);
            // fold trailing sub-mB residue into the final extraction so the tank can empty
            if (data.getAmount() > 0 && data.getAmount() < BlockEntityTank.DROPLETS_PER_MB)
                data.setAmount(0);
        }

        return extracted;
    }
}
