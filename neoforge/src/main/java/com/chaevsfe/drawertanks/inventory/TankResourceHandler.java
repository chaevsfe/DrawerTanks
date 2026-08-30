package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
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

    private final BlockEntityTank tank;
    private final SnapshotJournal<State> journal;
    private final RootCommitJournal commitJournal;

    private TankResourceHandler (BlockEntityTank tank) {
        this.tank = tank;
        this.commitJournal = new RootCommitJournal(tank::onContentsChanged);
        this.journal = new SnapshotJournal<>()
        {
            @Override
            protected State createSnapshot () {
                TankData data = tank.tankData();
                return new State(data.getFluid(), data.getComponents(), data.getAmount());
            }

            @Override
            protected void revertToSnapshot (State snapshot) {
                TankData data = tank.tankData();
                data.setFluid(snapshot.fluid(), snapshot.components());
                data.setAmount(snapshot.amount());
            }
        };
    }

    public static TankResourceHandler of (BlockEntityTank tank) {
        if (tank.platformFluidHandler() instanceof TankResourceHandler handler)
            return handler;

        TankResourceHandler handler = new TankResourceHandler(tank);
        tank.setPlatformFluidHandler(handler);
        return handler;
    }

    @Override
    public int size () {
        return 1;
    }

    @Override
    public FluidResource getResource (int index) {
        TankData data = tank.tankData();
        return data.isEmpty() ? FluidResource.EMPTY : FluidResource.of(data.getFluid(), data.getComponents());
    }

    @Override
    public long getAmountAsLong (int index) {
        return tank.tankData().getAmount() / BlockEntityTank.DROPLETS_PER_MB;
    }

    @Override
    public long getCapacityAsLong (int index, FluidResource resource) {
        return tank.capacityDroplets() / BlockEntityTank.DROPLETS_PER_MB;
    }

    @Override
    public boolean isValid (int index, FluidResource resource) {
        if (resource.isEmpty())
            return false;

        TankData data = tank.tankData();
        return data.isEmpty() || data.matches(resource.value(), resource.getComponentsPatch());
    }

    @Override
    public int insert (int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        TankData data = tank.tankData();
        if (!data.isEmpty() && !data.matches(resource.value(), resource.getComponentsPatch()))
            return 0;

        long spaceMb = Math.max(0, tank.capacityDroplets() - data.getAmount()) / BlockEntityTank.DROPLETS_PER_MB;
        int accepted = (int) Math.min(amount, spaceMb);

        if (accepted > 0) {
            journal.updateSnapshots(transaction);
            commitJournal.updateSnapshots(transaction);
            data.setFluid(resource.value(), resource.getComponentsPatch());
            data.setAmount(data.getAmount() + accepted * BlockEntityTank.DROPLETS_PER_MB);
        }

        if (tank.isVoid() && !data.isEmpty() && data.matches(resource.value(), resource.getComponentsPatch()))
            return amount;

        return accepted;
    }

    @Override
    public int extract (int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0)
            return 0;

        TankData data = tank.tankData();
        if (data.isEmpty() || !data.matches(resource.value(), resource.getComponentsPatch()))
            return 0;

        if (tank.isUnlimitedVending())
            return amount;

        long availableMb = data.getAmount() / BlockEntityTank.DROPLETS_PER_MB;
        int extracted = (int) Math.min(amount, availableMb);

        if (extracted > 0) {
            journal.updateSnapshots(transaction);
            commitJournal.updateSnapshots(transaction);
            data.setAmount(data.getAmount() - extracted * BlockEntityTank.DROPLETS_PER_MB);
        }

        return extracted;
    }
}
