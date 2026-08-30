package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.item.ItemFramedTank;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs
{
    private static final ResourceKey<CreativeModeTab> SD_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
        Identifier.fromNamespaceAndPath("storagedrawers", "storagedrawers"));

    public static void init () {
        CreativeModeTabEvents.modifyOutputEvent(SD_TAB).register(output ->
            ModItems.ITEMS.getEntries().forEach(reg -> {
                if (!(reg.get() instanceof ItemFramedTank))
                    output.accept(new ItemStack(reg.get()));
            }));
    }
}
