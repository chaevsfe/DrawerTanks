package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.block.tile.TankTarget;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

public class TankFluidStorage extends SnapshotParticipant<TankFluidStorage.State> implements SingleSlotStorage<FluidVariant>
{
    public record State(Fluid fluid, DataComponentPatch components, long amount) { }

    private final TankTarget tank;

    private TankFluidStorage (TankTarget tank) {
        this.tank = tank;
    }

    public static TankFluidStorage of (BlockEntityTank tank) {
        TankTarget target = tank.target();
        if (target.platformHandler instanceof TankFluidStorage storage)
            return storage;

        TankFluidStorage storage = new TankFluidStorage(target);
        target.platformHandler = storage;
        return storage;
    }

    @Override
    public long insert (FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0)
            return 0;

        TankData data = tank.data();
        if (data.hasFluid() && !data.matches(resource.getFluid(), resource.getComponentsPatch()))
            return 0;

        long space = Math.max(0, tank.capacity() - data.getAmount());
        long accepted = Math.min(maxAmount, space);

        if (accepted > 0) {
            updateSnapshots(transaction);
            data.setFluid(resource.getFluid(), resource.getComponentsPatch());
            data.setAmount(data.getAmount() + accepted);
        }

        if (tank.isVoid() && data.hasFluid() && data.matches(resource.getFluid(), resource.getComponentsPatch()))
            return maxAmount;

        return accepted;
    }

    @Override
    public long extract (FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0)
            return 0;

        TankData data = tank.data();
        if (!data.hasFluid() || !data.matches(resource.getFluid(), resource.getComponentsPatch()))
            return 0;

        if (tank.isUnlimitedVending())
            return maxAmount;

        long extracted = Math.min(maxAmount, data.getAmount());
        if (extracted > 0) {
            updateSnapshots(transaction);
            data.setAmount(data.getAmount() - extracted);
        }

        return extracted;
    }

    @Override
    public boolean isResourceBlank () {
        return !tank.data().hasFluid();
    }

    @Override
    public FluidVariant getResource () {
        TankData data = tank.data();
        return data.hasFluid() ? FluidVariant.of(data.getFluid(), data.getComponents()) : FluidVariant.blank();
    }

    @Override
    public long getAmount () {
        return tank.data().getAmount();
    }

    @Override
    public long getCapacity () {
        return tank.capacity();
    }

    @Override
    protected State createSnapshot () {
        TankData data = tank.data();
        return new State(data.getFluid(), data.getComponents(), data.getAmount());
    }

    @Override
    protected void readSnapshot (State snapshot) {
        TankData data = tank.data();
        data.setFluid(snapshot.fluid(), snapshot.components());
        data.setAmount(snapshot.amount());
    }

    @Override
    protected void onFinalCommit () {
        tank.onChanged();
    }
}
