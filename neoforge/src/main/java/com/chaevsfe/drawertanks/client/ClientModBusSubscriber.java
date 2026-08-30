package com.chaevsfe.drawertanks.client;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.client.renderer.BlockEntityTankRenderer;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModContainers;
import com.chaevsfe.drawertanks.inventory.TankScreen;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.chaevsfe.drawertanks.platform.ClientFluidBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

@EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT)
public class ClientModBusSubscriber
{
    @SubscribeEvent
    public static void registerEntityRenderers (EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TANK.get(), BlockEntityTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LINKED_TANK.get(), BlockEntityTankRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens (RegisterMenuScreensEvent event) {
        event.register(ModContainers.TANK_CONTAINER.get(), TankScreen::new);
    }

    @SubscribeEvent
    public static void clientSetup (FMLClientSetupEvent event) {
        Bridges.CLIENT_FLUID = new ClientFluidBridge()
        {
            @Override
            public int color (Fluid fluid, DataComponentPatch components) {
                FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
                if (model == null)
                    return 0xFFFFFFFF;
                return model.fluidTintSource().colorAsStack(FluidResource.of(fluid, components).toStack(1));
            }

            @Override
            public int luminance (Fluid fluid, DataComponentPatch components) {
                return fluid.getFluidType().getLightLevel();
            }
        };
    }
}
