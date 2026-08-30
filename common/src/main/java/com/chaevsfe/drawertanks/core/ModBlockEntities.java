package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;
import java.util.stream.Collectors;

public final class ModBlockEntities
{
    public static final ChameleonRegistry<BlockEntityType<?>> BLOCK_ENTITIES = ChameleonServices.REGISTRY.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModConstants.MOD_ID);

    public static final RegistryEntry<BlockEntityType<BlockEntityTank>> TANK = BLOCK_ENTITIES.register("tank", () ->
        new BlockEntityType<>(BlockEntityTank::new, ModBlocks.TANKS.stream().map(RegistryEntry::get).collect(Collectors.toSet())));

    private ModBlockEntities () { }

    public static void init (ChameleonInit.InitContext context) {
        BLOCK_ENTITIES.init(context);
    }
}
