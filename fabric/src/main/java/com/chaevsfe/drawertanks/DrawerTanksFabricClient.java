package com.chaevsfe.drawertanks;

import com.chaevsfe.drawertanks.client.renderer.BlockEntityTankRenderer;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModContainers;
import com.chaevsfe.drawertanks.inventory.TankScreen;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.chaevsfe.drawertanks.platform.ClientFluidBridge;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

public class DrawerTanksFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient () {
        BlockEntityRenderers.register(ModBlockEntities.TANK.get(), BlockEntityTankRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.LINKED_TANK.get(), BlockEntityTankRenderer::new);

        MenuScreens.register(ModContainers.TANK_CONTAINER.get(), TankScreen::new);

        Bridges.CLIENT_FLUID = new ClientFluidBridge()
        {
            @Override
            public int color (Fluid fluid, DataComponentPatch components) {
                return FluidVariantRendering.getColor(FluidVariant.of(fluid, components));
            }

            @Override
            public int luminance (Fluid fluid, DataComponentPatch components) {
                return FluidVariantAttributes.getLuminance(FluidVariant.of(fluid, components));
            }
        };
    }
}
