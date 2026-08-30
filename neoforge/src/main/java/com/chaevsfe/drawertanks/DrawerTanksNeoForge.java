package com.chaevsfe.drawertanks;

import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModBlocks;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.chaevsfe.drawertanks.core.ModItems;
import com.chaevsfe.drawertanks.inventory.TankResourceHandler;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.chaevsfe.drawertanks.platform.FluidBridge;
import com.texelsaurus.minecraft.chameleon.registry.NeoforgeRegistryContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

@Mod(ModConstants.MOD_ID)
public class DrawerTanksNeoForge
{
    private static final ResourceKey<CreativeModeTab> SD_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
        Identifier.fromNamespaceAndPath("storagedrawers", "storagedrawers"));

    public DrawerTanksNeoForge (ModContainer modContainer, IEventBus modEventBus) {
        NeoforgeRegistryContext context = new NeoforgeRegistryContext(modEventBus);

        ModBlocks.init(context);
        ModItems.init(context);
        ModBlockEntities.init(context);
        ModDataComponents.init(context);

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::buildCreativeTabs);

        Bridges.FLUID = new FluidBridge()
        {
            @Override
            public boolean isFluidContainer (ItemStack stack) {
                if (stack.isEmpty())
                    return false;
                return ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM) != null;
            }

            @Override
            public boolean interact (Player player, InteractionHand hand, Level level, BlockPos pos, Direction side) {
                return FluidUtil.interactWithFluidHandler(player, hand, level, pos, side, null);
            }
        };
    }

    private void registerCapabilities (RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.TANK.get(), (be, side) -> TankResourceHandler.of(be));
    }

    private void buildCreativeTabs (BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(SD_TAB))
            return;

        ModItems.ITEMS.getEntries().forEach(reg -> event.accept(reg.get()));
    }
}
