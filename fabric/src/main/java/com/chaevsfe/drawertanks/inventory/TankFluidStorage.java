package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
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

    private final BlockEntityTank tank;

    private TankFluidStorage (BlockEntityTank tank) {
        this.tank = tank;
    }

    public static TankFluidStorage of (BlockEntityTank tank) {
        if (tank.platformFluidHandler() instanceof TankFluidStorage storage)
            return storage;

        TankFluidStorage storage = new TankFluidStorage(tank);
        tank.setPlatformFluidHandler(storage);
        return storage;
    }

    @Override
    public long insert (FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0)
            return 0;

        TankData data = tank.tankData();
        if (!data.isEmpty() && !data.matches(resource.getFluid(), resource.getComponentsPatch()))
            return 0;

        long space = Math.max(0, tank.capacityDroplets() - data.getAmount());
        long accepted = Math.min(maxAmount, space);

        if (accepted > 0) {
            updateSnapshots(transaction);
            data.setFluid(resource.getFluid(), resource.getComponentsPatch());
            data.setAmount(data.getAmount() + accepted);
        }

        if (tank.isVoid() && !data.isEmpty() && data.matches(resource.getFluid(), resource.getComponentsPatch()))
            return maxAmount;

        return accepted;
    }

    @Override
    public long extract (FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0)
            return 0;

        TankData data = tank.tankData();
        if (data.isEmpty() || !data.matches(resource.getFluid(), resource.getComponentsPatch()))
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
        return tank.tankData().isEmpty();
    }

    @Override
    public FluidVariant getResource () {
        TankData data = tank.tankData();
        return data.isEmpty() ? FluidVariant.blank() : FluidVariant.of(data.getFluid(), data.getComponents());
    }

    @Override
    public long getAmount () {
        return tank.tankData().getAmount();
    }

    @Override
    public long getCapacity () {
        return tank.capacityDroplets();
    }

    @Override
    protected State createSnapshot () {
        TankData data = tank.tankData();
        return new State(data.getFluid(), data.getComponents(), data.getAmount());
    }

    @Override
    protected void readSnapshot (State snapshot) {
        TankData data = tank.tankData();
        data.setFluid(snapshot.fluid(), snapshot.components());
        data.setAmount(snapshot.amount());
    }

    @Override
    protected void onFinalCommit () {
        tank.onContentsChanged();
    }
}
