package com.chaevsfe.drawertanks;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.config.FabricTankConfig;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModBlocks;
import com.chaevsfe.drawertanks.core.ModContainers;
import com.chaevsfe.drawertanks.core.ModCreativeTabs;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.chaevsfe.drawertanks.core.ModItems;
import com.chaevsfe.drawertanks.inventory.TankFluidStorage;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.chaevsfe.drawertanks.platform.FluidBridge;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
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
}
