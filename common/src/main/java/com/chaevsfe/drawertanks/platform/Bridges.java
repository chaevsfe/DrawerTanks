package com.chaevsfe.drawertanks.platform;

import com.chaevsfe.drawertanks.block.tile.BlockEntityFramedTank;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Consumer;

public final class Bridges
{
    public static FluidBridge FLUID;
    public static ClientFluidBridge CLIENT_FLUID;
    public static BlockEntityType.BlockEntitySupplier<BlockEntityFramedTank> FRAMED_TANK_FACTORY = BlockEntityFramedTank::new;

    // a linked block's handler belongs to its channel pool, so re-dyeing it hands out a different one
    public static Consumer<BlockEntity> INVALIDATE_CAPS = be -> { };

    private Bridges () { }
}
