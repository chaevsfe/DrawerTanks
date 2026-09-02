package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.components.LinkFluid;
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
import net.minecraft.world.item.ItemStackTemplate;

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

    // display-only snapshots of a linked channel, taken as the block breaks, so the item can be told
    // apart in an inventory; the contents themselves never leave the channel
    // both are value types on purpose: a raw ItemStack has identity equality and would stop
    // same-channel drops from stacking, and an amount would do the same for tanks
    public static final RegistryEntry<DataComponentType<LinkFluid>> LINK_FLUID =
        COMPONENTS.register("link_fluid", () -> DataComponentType.<LinkFluid>builder()
            .persistent(LinkFluid.CODEC).networkSynchronized(LinkFluid.STREAM_CODEC).build());

    public static final RegistryEntry<DataComponentType<ItemStackTemplate>> LINK_ITEM =
        COMPONENTS.register("link_item", () -> DataComponentType.<ItemStackTemplate>builder()
            .persistent(ItemStackTemplate.CODEC).networkSynchronized(ItemStackTemplate.STREAM_CODEC).build());

    private ModDataComponents () { }

    public static void init (ChameleonInit.InitContext context) {
        COMPONENTS.init(context);
    }
}
