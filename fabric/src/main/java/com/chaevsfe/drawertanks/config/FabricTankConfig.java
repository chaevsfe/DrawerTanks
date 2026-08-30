package com.chaevsfe.drawertanks.config;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class FabricTankConfig
{
    private static final ModConfigSpec SPEC;
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

    private FabricTankConfig () { }

    public static void init () {
        ModConfigEvents.loading("drawertanks").register(config -> apply());
        ModConfigEvents.reloading("drawertanks").register(config -> apply());
        ConfigRegistry.INSTANCE.register("drawertanks", ModConfig.Type.COMMON, SPEC, "drawertanks-common.toml");
        if (SPEC.isLoaded())
            apply();
    }

    private static void apply () {
        TankConfig.baseCapacityBuckets = BASE_CAPACITY.get();
        TankConfig.linkedTransferMbPerTick = LINKED_RATE.get();
    }
}
