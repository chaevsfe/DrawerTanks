package com.chaevsfe.drawertanks.client;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.client.model.TankMaterialDecorator;
import com.chaevsfe.drawertanks.client.renderer.BlockEntityTankRenderer;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModBlocks;
import com.chaevsfe.drawertanks.core.ModContainers;
import com.chaevsfe.drawertanks.inventory.TankScreen;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.FramedModelProperties;
import com.jaquadro.minecraft.storagedrawers.client.model.ItemModelStore;
import com.jaquadro.minecraft.storagedrawers.client.model.PlatformDecoratedModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.chaevsfe.drawertanks.platform.ClientFluidBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
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
        event.registerBlockEntityRenderer(ModBlockEntities.FRAMED_TANK.get(), BlockEntityTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LINKED_DRAWER.get(), com.chaevsfe.drawertanks.client.renderer.BlockEntityLinkedDrawerRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens (RegisterMenuScreensEvent event) {
        event.register(ModContainers.TANK_CONTAINER.get(), TankScreen::new);
    }

    // LOW priority so SD's default-priority handler has already cleared ItemModelStore
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void registerModels (ModelEvent.ModifyBakingResult event) {
        for (BlockState state : ModBlocks.FRAMED_TANK.get().getStateDefinition().getPossibleStates()) {
            BlockStateModel original = event.getBakingResult().blockStateModels().get(state);
            if (original == null || original instanceof PlatformDecoratedModel)
                continue;

            BlockStateModel proxy = new PlatformDecoratedModel<>(original, new TankMaterialDecorator(), FramedModelProperties.INSTANCE);
            ItemModelStore.models.put(state, proxy);
            event.getBakingResult().blockStateModels().put(state, proxy);
        }
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
                // lava, milk and any fluid registered without a tint have a null source
                var tint = model.fluidTintSource();
                if (tint == null)
                    return 0xFFFFFFFF;
                return tint.colorAsStack(FluidResource.of(fluid, components).toStack(1));
            }

            @Override
            public int luminance (Fluid fluid, DataComponentPatch components) {
                return fluid.getFluidType().getLightLevel();
            }

            @Override
            public Component fluidName (Fluid fluid, DataComponentPatch components) {
                return FluidResource.of(fluid, components).getHoverName();
            }
        };
    }
}
