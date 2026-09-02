package com.chaevsfe.drawertanks;

import com.chaevsfe.drawertanks.config.NeoforgeTankConfig;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModBlocks;
import com.chaevsfe.drawertanks.core.ModContainers;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.chaevsfe.drawertanks.core.ModItems;
import com.chaevsfe.drawertanks.inventory.TankResourceHandler;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.chaevsfe.drawertanks.platform.FluidBridge;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.texelsaurus.minecraft.chameleon.capabilities.ChameleonCapability;
import com.texelsaurus.minecraft.chameleon.capabilities.NeoforgeCapability;
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
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
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
        Bridges.FRAMED_TANK_FACTORY = com.chaevsfe.drawertanks.block.tile.NeoforgeBlockEntityFramedTank::new;

        // SERVER so the values are synced to clients; capacity is read for rendering and tooltips
        modContainer.registerConfig(ModConfig.Type.SERVER, NeoforgeTankConfig.SPEC, "drawertanks-server.toml");
        modEventBus.addListener((ModConfigEvent event) -> {
            if (event.getConfig().getSpec() == NeoforgeTankConfig.SPEC)
                NeoforgeTankConfig.apply();
        });

        Bridges.INVALIDATE_CAPS = be -> {
            if (be.getLevel() != null && !be.getLevel().isClientSide())
                be.invalidateCapabilities();
        };

        NeoforgeRegistryContext context = new NeoforgeRegistryContext(modEventBus);

        ModBlocks.init(context);
        ModItems.init(context);
        ModBlockEntities.init(context);
        ModContainers.init(context);
        ModDataComponents.init(context);

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::buildCreativeTabs);

        // highest priority, so another mod's block-use handler cannot swallow the click first
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) -> {
                var result = com.chaevsfe.drawertanks.block.OffhandMenuOpen.tryOpen(
                    event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
                if (result != net.minecraft.world.InteractionResult.PASS) {
                    event.setCanceled(true);
                    event.setCancellationResult(result);
                }
            });

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) -> {
                var state = event.getLevel().getBlockState(event.getPos());
                if (state.getBlock() instanceof com.chaevsfe.drawertanks.block.BlockLinkedDrawer block
                    && event.getFace() == state.getValue(com.chaevsfe.drawertanks.block.BlockLinkedDrawer.FACING)) {
                    event.setCanceled(true);
                    if (!event.getLevel().isClientSide()
                        && event.getAction() == net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START)
                        block.takeItem(event.getLevel(), event.getPos(), event.getEntity(), !event.getEntity().isShiftKeyDown());
                }
            });

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
                // FluidUtil plays its sound for every player from the server, so the client must not repeat it
                if (level.isClientSide())
                    return true;

                return FluidUtil.interactWithFluidHandler(player, hand, level, pos, side, null);
            }
        };
    }

    private void registerCapabilities (RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.TANK.get(), (be, side) -> TankResourceHandler.of(be));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.LINKED_TANK.get(), (be, side) -> TankResourceHandler.of(be));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FRAMED_TANK.get(), (be, side) -> TankResourceHandler.of(be));

        DrawerTanksNeoForge.<IDrawerAttributes, Object>cast(com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities.DRAWER_ATTRIBUTES)
            .register(event, ModBlockEntities.TANK.get(), (e, c) -> e.getDrawerAttributes());
        DrawerTanksNeoForge.<IDrawerAttributes, Object>cast(com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities.DRAWER_ATTRIBUTES)
            .register(event, ModBlockEntities.LINKED_TANK.get(), (e, c) -> e.getDrawerAttributes());
        DrawerTanksNeoForge.<IDrawerAttributes, Object>cast(com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities.DRAWER_ATTRIBUTES)
            .register(event, ModBlockEntities.FRAMED_TANK.get(), (e, c) -> e.getDrawerAttributes());

        DrawerTanksNeoForge.<IDrawerAttributes, Object>cast(com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities.DRAWER_ATTRIBUTES)
            .register(event, ModBlockEntities.LINKED_DRAWER.get(), (e, c) -> e.getDrawerAttributes());

        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.LINKED_DRAWER.get(),
            (be, side) -> com.chaevsfe.drawertanks.inventory.LinkedDrawerResourceHandler.of(be));
    }

    @SuppressWarnings("unchecked")
    private static <T, C> NeoforgeCapability<T, C> cast (ChameleonCapability<T> cap) {
        return (NeoforgeCapability<T, C>) cap;
    }

    private void buildCreativeTabs (BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(SD_TAB))
            return;

        ModItems.ITEMS.getEntries().forEach(reg -> {
            if (!(reg.get() instanceof com.chaevsfe.drawertanks.item.ItemFramedTank))
                event.accept(reg.get());
        });
    }
}
