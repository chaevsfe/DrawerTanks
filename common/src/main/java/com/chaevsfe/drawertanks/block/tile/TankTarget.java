package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;

// The fluid state a platform handler operates on. A plain tank owns one of these; every linked tank
// on a dye channel shares the pool's, so exactly one transaction participant ever snapshots a pool.
public abstract class TankTarget
{
    public Object platformHandler;

    public abstract TankData data ();

    public abstract long capacity ();

    public abstract void onChanged ();

    public boolean isVoid () {
        return false;
    }

    public boolean isUnlimitedVending () {
        return false;
    }

    public boolean isFluidLocked () {
        return false;
    }
}
