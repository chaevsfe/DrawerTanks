package com.chaevsfe.drawertanks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class NeoforgeTankConfig
{
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue BASE_CAPACITY;
    private static final ModConfigSpec.IntValue LINKED_CAPACITY;
    private static final ModConfigSpec.IntValue LINKED_ITEM_CAPACITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        BASE_CAPACITY = builder.comment("Base tank capacity in buckets before storage upgrades.")
            .defineInRange("baseCapacityBuckets", 8, 1, 65536);
        LINKED_CAPACITY = builder.comment("Shared capacity of each linked tank dye channel, in buckets.")
            .defineInRange("linkedChannelCapacityBuckets", 16, 1, 65536);
        LINKED_ITEM_CAPACITY = builder.comment("Shared capacity of each linked drawer dye channel, in stacks.")
            .defineInRange("linkedChannelCapacityStacks", 32, 1, 65536);
        SPEC = builder.build();
    }

    private NeoforgeTankConfig () { }

    public static void apply () {
        TankConfig.baseCapacityBuckets = BASE_CAPACITY.get();
        TankConfig.linkedChannelCapacityBuckets = LINKED_CAPACITY.get();
        TankConfig.linkedChannelCapacityStacks = LINKED_ITEM_CAPACITY.get();
    }
}
