package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.block.BlockFramedTank;
import com.chaevsfe.drawertanks.block.BlockLinkedDrawer;
import com.chaevsfe.drawertanks.block.BlockLinkedTank;
import com.chaevsfe.drawertanks.block.BlockTank;
import com.jaquadro.minecraft.storagedrawers.block.meta.BlockMeta;
import com.jaquadro.minecraft.storagedrawers.block.meta.BlockMetaFacing;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

public final class ModBlocks
{
    public static final ChameleonRegistry<Block> BLOCKS = ChameleonServices.REGISTRY.create(BuiltInRegistries.BLOCK, ModConstants.MOD_ID);

    public static final String[] WOODS = {
        "acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak",
        "jungle", "mangrove", "oak", "pale_oak", "spruce", "warped"
    };

    public static final List<RegistryEntry<BlockTank>> TANKS = new ArrayList<>();

    static {
        for (String wood : WOODS)
            TANKS.add(registerTank(wood + "_tank"));
    }

    public static final RegistryEntry<BlockLinkedTank> LINKED_TANK = BLOCKS.register("linked_tank",
        () -> new BlockLinkedTank(Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(10f, 600f)
            .sound(SoundType.WOOD)
            .setId(ResourceKey.create(Registries.BLOCK, ModConstants.loc("linked_tank")))));

    public static final RegistryEntry<BlockLinkedDrawer> LINKED_DRAWER = BLOCKS.register("linked_drawer",
        () -> new BlockLinkedDrawer(Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(10f, 600f)
            .sound(SoundType.WOOD)
            .setId(ResourceKey.create(Registries.BLOCK, ModConstants.loc("linked_drawer")))));

    public static final List<String> EXCLUDE_ITEMS = new ArrayList<>();

    public static final RegistryEntry<BlockFramedTank> FRAMED_TANK = BLOCKS.register("framed_tank",
        () -> new BlockFramedTank(Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5f)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .setId(ResourceKey.create(Registries.BLOCK, ModConstants.loc("framed_tank")))));

    public static final RegistryEntry<BlockMeta> META_TANK_SIDE = registerMetaFacingBlock("meta_tank_side");
    public static final RegistryEntry<BlockMeta> META_TANK_TRIM = registerMetaBlock("meta_tank_trim");

    private static RegistryEntry<BlockMeta> registerMetaFacingBlock (String name) {
        EXCLUDE_ITEMS.add(name);
        return BLOCKS.register(name, () -> new BlockMetaFacing(Properties.of().air()
            .setId(ResourceKey.create(Registries.BLOCK, ModConstants.loc(name)))));
    }

    private static RegistryEntry<BlockMeta> registerMetaBlock (String name) {
        EXCLUDE_ITEMS.add(name);
        return BLOCKS.register(name, () -> new BlockMeta(Properties.of().air()
            .setId(ResourceKey.create(Registries.BLOCK, ModConstants.loc(name)))));
    }

    private ModBlocks () { }

    private static RegistryEntry<BlockTank> registerTank (String name) {
        return BLOCKS.register(name, () -> new BlockTank(Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5f)
            .sound(SoundType.WOOD)
            .setId(ResourceKey.create(Registries.BLOCK, ModConstants.loc(name)))));
    }

    public static void init (ChameleonInit.InitContext context) {
        BLOCKS.init(context);
    }
}
