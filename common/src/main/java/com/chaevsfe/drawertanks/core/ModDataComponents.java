package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.components.TankContents;
import com.chaevsfe.drawertanks.components.TankUpgrades;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModDataComponents
{
    public static final ChameleonRegistry<DataComponentType<?>> COMPONENTS = ChameleonServices.REGISTRY.create(BuiltInRegistries.DATA_COMPONENT_TYPE, ModConstants.MOD_ID);

    public static final RegistryEntry<DataComponentType<TankContents>> TANK_CONTENTS =
        COMPONENTS.register("tank_contents", () -> DataComponentType.<TankContents>builder()
            .persistent(TankContents.CODEC).networkSynchronized(TankContents.STREAM_CODEC).build());

    public static final RegistryEntry<DataComponentType<TankUpgrades>> TANK_UPGRADES =
        COMPONENTS.register("tank_upgrades", () -> DataComponentType.<TankUpgrades>builder()
            .persistent(TankUpgrades.CODEC).build());

    public static final RegistryEntry<DataComponentType<GlobalPos>> COUPLER_TARGET =
        COMPONENTS.register("coupler_target", () -> DataComponentType.<GlobalPos>builder()
            .persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    private ModDataComponents () { }

    public static void init (ChameleonInit.InitContext context) {
        COMPONENTS.init(context);
    }
}
