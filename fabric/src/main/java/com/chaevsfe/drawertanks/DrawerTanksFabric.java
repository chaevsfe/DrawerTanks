package com.chaevsfe.drawertanks;

import com.chaevsfe.drawertanks.block.BlockLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.config.FabricTankConfig;
import com.chaevsfe.drawertanks.inventory.LinkedDrawerItemStorage;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModBlocks;
import com.chaevsfe.drawertanks.core.ModContainers;
import com.chaevsfe.drawertanks.core.ModCreativeTabs;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.chaevsfe.drawertanks.core.ModItems;
import com.chaevsfe.drawertanks.inventory.TankFluidStorage;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.chaevsfe.drawertanks.platform.FluidBridge;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.capabilities.IFabricCapability;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DrawerTanksFabric implements ModInitializer
{
    @Override
    public void onInitialize () {
        if (FabricLoader.getInstance().isModLoaded("forgeconfigapiport"))
            FabricTankConfig.init();

        ChameleonInit.InitContext context = new ChameleonInit.InitContext();

        ModBlocks.init(context);
        ModItems.init(context);
        ModBlockEntities.init(context);
        ModContainers.init(context);
        ModDataComponents.init(context);

        ModCreativeTabs.init();

        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> TankFluidStorage.of(be), ModBlockEntities.TANK.get());
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> TankFluidStorage.of(be), ModBlockEntities.LINKED_TANK.get());
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> TankFluidStorage.of(be), ModBlockEntities.FRAMED_TANK.get());
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> LinkedDrawerItemStorage.of(be), ModBlockEntities.LINKED_DRAWER.get());

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || player.getItemInHand(hand).isEmpty())
                return InteractionResult.PASS;

            var pos = hitResult.getBlockPos();
            if (!(world.getBlockState(pos).getBlock() instanceof BlockLinkedDrawer block))
                return InteractionResult.PASS;

            return block.putItems(world, pos, player);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockLinkedDrawer block
                && direction == state.getValue(BlockLinkedDrawer.FACING)) {
                if (!world.isClientSide())
                    block.takeItem(world, pos, player, player.isShiftKeyDown());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        // Chameleon's DeferredCapability silently drops a register() call until Storage Drawers has
        // published the backing capability from its own initializer, so run this again once every
        // mod's entrypoint has finished.
        registerDrawerAttributes();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> registerDrawerAttributes());

        Bridges.FLUID = new FluidBridge()
        {
            @Override
            public boolean isFluidContainer (ItemStack stack) {
                if (stack.isEmpty())
                    return false;
                return ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null;
            }

            @Override
            public boolean interact (Player player, InteractionHand hand, Level level, BlockPos pos, Direction side) {
                if (!(level.getBlockEntity(pos) instanceof BlockEntityTank tank))
                    return false;

                return FluidStorageUtil.interactWithFluidStorage(TankFluidStorage.of(tank), player, hand);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static void registerDrawerAttributes () {
        IFabricCapability<IDrawerAttributes> capability = (IFabricCapability<IDrawerAttributes>) Capabilities.DRAWER_ATTRIBUTES;
        capability.register(ModBlockEntities.TANK.get(), be -> be.getDrawerAttributes());
        capability.register(ModBlockEntities.LINKED_TANK.get(), be -> be.getDrawerAttributes());
        capability.register(ModBlockEntities.FRAMED_TANK.get(), be -> be.getDrawerAttributes());
    }
}
