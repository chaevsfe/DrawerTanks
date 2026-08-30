package com.chaevsfe.drawertanks.config;

public final class TankConfig
{
    public static volatile int baseCapacityBuckets = 8;
    public static volatile int linkedChannelCapacityBuckets = 16;
    public static volatile int linkedChannelCapacityStacks = 32;

    private TankConfig () { }
}
