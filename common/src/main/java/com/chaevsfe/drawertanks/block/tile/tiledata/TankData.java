package com.chaevsfe.drawertanks.block.tile.tiledata;

import com.chaevsfe.drawertanks.components.TankContents;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.BlockEntityDataShim;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TankData extends BlockEntityDataShim
{
    private Fluid fluid = Fluids.EMPTY;
    private DataComponentPatch components = DataComponentPatch.EMPTY;
    private long amount;

    public Fluid getFluid () {
        return fluid;
    }

    public DataComponentPatch getComponents () {
        return components;
    }

    public long getAmount () {
        return amount;
    }

    public boolean isEmpty () {
        return fluid == Fluids.EMPTY || amount <= 0;
    }

    public boolean matches (Fluid otherFluid, DataComponentPatch otherComponents) {
        return fluid == otherFluid && components.equals(otherComponents);
    }

    public void setFluid (Fluid fluid, DataComponentPatch components) {
        this.fluid = fluid;
        this.components = components;
    }

    public void setAmount (long amount) {
        this.amount = amount;
        if (this.amount <= 0)
            clear();
    }

    public void clear () {
        fluid = Fluids.EMPTY;
        components = DataComponentPatch.EMPTY;
        amount = 0;
    }

    public TankContents toContents () {
        return isEmpty() ? TankContents.EMPTY : new TankContents(fluid, components, amount);
    }

    public void fromContents (TankContents contents) {
        if (contents == null || contents.isEmpty()) {
            clear();
            return;
        }

        fluid = contents.fluid();
        components = contents.components();
        amount = contents.amount();
    }

    @Override
    public void read (ValueInput input) {
        fluid = input.read("Fluid", BuiltInRegistries.FLUID.byNameCodec()).orElse(Fluids.EMPTY);
        components = input.read("FluidComponents", DataComponentPatch.CODEC).orElse(DataComponentPatch.EMPTY);
        amount = input.read("Amount", Codec.LONG).orElse(0L);
        if (isEmpty())
            clear();
    }

    @Override
    public void write (ValueOutput output) {
        if (isEmpty()) {
            output.discard("Fluid");
            output.discard("FluidComponents");
            output.discard("Amount");
            return;
        }

        output.store("Fluid", BuiltInRegistries.FLUID.byNameCodec(), fluid);
        if (components != DataComponentPatch.EMPTY)
            output.store("FluidComponents", DataComponentPatch.CODEC, components);
        else
            output.discard("FluidComponents");
        output.store("Amount", Codec.LONG, amount);
    }
}
