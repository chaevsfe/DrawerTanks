package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.item.ItemTankCoupler;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModItems
{
    public static final ChameleonRegistry<Item> ITEMS = ChameleonServices.REGISTRY.create(BuiltInRegistries.ITEM, ModConstants.MOD_ID);

    private ModItems () { }

    public static final RegistryEntry<ItemTankCoupler> TANK_COUPLER = ITEMS.register("tank_coupler",
        () -> new ItemTankCoupler(new Item.Properties()
            .stacksTo(1)
            .setId(modKey(ModConstants.loc("tank_coupler")))));

    public static void init (ChameleonInit.InitContext context) {
        for (RegistryEntry<Block> ro : ModBlocks.BLOCKS.getEntries())
            registerBlock(ro);

        ITEMS.init(context);
    }

    private static void registerBlock (RegistryEntry<? extends Block> blockHolder) {
        ITEMS.register(blockHolder.getId().getPath(), () -> {
            Item.Properties itemProperties = new Item.Properties()
                .useBlockDescriptionPrefix()
                .setId(modKey(blockHolder.getId()));
            return new BlockItem(blockHolder.get(), itemProperties);
        });
    }

    private static ResourceKey<Item> modKey (Identifier name) {
        return ResourceKey.create(Registries.ITEM, name);
    }
}
