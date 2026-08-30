package com.chaevsfe.drawertanks.platform;

import com.chaevsfe.drawertanks.block.tile.BlockEntityFramedTank;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class Bridges
{
    public static FluidBridge FLUID;
    public static ClientFluidBridge CLIENT_FLUID;
    public static BlockEntityType.BlockEntitySupplier<BlockEntityFramedTank> FRAMED_TANK_FACTORY = BlockEntityFramedTank::new;

    private Bridges () { }
}
