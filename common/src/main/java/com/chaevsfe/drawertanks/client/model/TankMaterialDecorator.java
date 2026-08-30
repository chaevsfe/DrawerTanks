package com.chaevsfe.drawertanks.client.model;

import com.chaevsfe.drawertanks.core.ModBlocks;
import com.jaquadro.minecraft.storagedrawers.client.model.DrawerModelStore;
import com.jaquadro.minecraft.storagedrawers.client.model.context.FramedModelContext;
import com.jaquadro.minecraft.storagedrawers.client.model.decorator.MaterialModelDecorator;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Supplier;

public class TankMaterialDecorator extends MaterialModelDecorator<FramedModelContext>
{
    // the DynamicPart value is only a routing token for getStoreModel; it is never looked up in SD's store
    private static final DrawerModelStore.FrameMatSet TANK_MATERIALS = new DrawerModelStore.FrameMatSet()
        .sidePart(DrawerModelStore.DynamicPart.FRAMED_TRIM_SIDE);

    public TankMaterialDecorator () {
        super(TANK_MATERIALS, false);
    }

    @Override
    protected BlockStateModel getStoreModel (FramedModelContext context, DrawerModelStore.DynamicPart part) {
        Direction facing = Direction.NORTH;
        BlockState state = context.state();
        if (state != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        BlockState meta = ModBlocks.META_TANK_SIDE.get().defaultBlockState();
        if (meta.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            meta = meta.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);

        return DrawerModelStore.getModel(meta);
    }

    // the window front and interior live in the base model and must stay visible when framed
    @Override
    public boolean shouldRenderBase (Supplier<FramedModelContext> contextSupplier) {
        return true;
    }
}
