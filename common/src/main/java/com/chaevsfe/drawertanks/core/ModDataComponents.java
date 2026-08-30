package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.components.TankAttributesData;
import com.chaevsfe.drawertanks.components.TankContents;
import com.chaevsfe.drawertanks.components.TankUpgrades;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.List;

public final class ModDataComponents
{
    public static final ChameleonRegistry<DataComponentType<?>> COMPONENTS = ChameleonServices.REGISTRY.create(BuiltInRegistries.DATA_COMPONENT_TYPE, ModConstants.MOD_ID);

    public static final RegistryEntry<DataComponentType<TankContents>> TANK_CONTENTS =
        COMPONENTS.register("tank_contents", () -> DataComponentType.<TankContents>builder()
            .persistent(TankContents.CODEC).networkSynchronized(TankContents.STREAM_CODEC).build());

    public static final RegistryEntry<DataComponentType<TankUpgrades>> TANK_UPGRADES =
        COMPONENTS.register("tank_upgrades", () -> DataComponentType.<TankUpgrades>builder()
            .persistent(TankUpgrades.CODEC).build());

    public static final RegistryEntry<DataComponentType<TankAttributesData>> TANK_ATTRIBUTES =
        COMPONENTS.register("tank_attributes", () -> DataComponentType.<TankAttributesData>builder()
            .persistent(TankAttributesData.CODEC).networkSynchronized(TankAttributesData.STREAM_CODEC).build());

    // the five dye ids of a linked block, so breaking one does not orphan its channel
    public static final RegistryEntry<DataComponentType<List<Integer>>> LINK_CHANNELS =
        COMPONENTS.register("link_channels", () -> DataComponentType.<List<Integer>>builder()
            .persistent(Codec.INT.listOf()).networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())).build());

    private ModDataComponents () { }

    public static void init (ChameleonInit.InitContext context) {
        COMPONENTS.init(context);
    }
}
