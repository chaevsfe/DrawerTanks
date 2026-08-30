package com.chaevsfe.drawertanks.core;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.inventory.ContainerTank;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.inventory.content.PositionContent;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class ModContainers
{
    public static final ChameleonRegistry<MenuType<?>> CONTAINERS = ChameleonServices.REGISTRY.create(BuiltInRegistries.MENU, ModConstants.MOD_ID);

    public static final RegistryEntry<MenuType<ContainerTank>> TANK_CONTAINER =
        CONTAINERS.register("tank", ChameleonServices.CONTAINER.getContainerSupplier(ContainerTank::new, PositionContent.SERIALIZER));

    private ModContainers () { }

    public static void init (ChameleonInit.InitContext context) {
        CONTAINERS.init(context);
    }
}
