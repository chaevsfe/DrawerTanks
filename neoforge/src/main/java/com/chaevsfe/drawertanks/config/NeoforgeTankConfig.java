package com.chaevsfe.drawertanks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoforgeTankConfig
{
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue BASE_CAPACITY;
    private static final ModConfigSpec.IntValue LINKED_RATE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        BASE_CAPACITY = builder.comment("Base tank capacity in buckets before storage upgrades.")
            .defineInRange("baseCapacityBuckets", 8, 1, 65536);
        LINKED_RATE = builder.comment("Linked tank transfer rate in millibuckets per tick.")
            .defineInRange("linkedTransferMbPerTick", 50, 1, 81000);
        SPEC = builder.build();
    }

    private NeoforgeTankConfig () { }

    public static void apply () {
        TankConfig.baseCapacityBuckets = BASE_CAPACITY.get();
        TankConfig.linkedTransferMbPerTick = LINKED_RATE.get();
    }
}
