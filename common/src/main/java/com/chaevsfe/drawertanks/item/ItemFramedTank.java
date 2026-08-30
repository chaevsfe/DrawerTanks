package com.chaevsfe.drawertanks.item;

import com.chaevsfe.drawertanks.block.tile.BlockEntityFramedTank;
import com.texelsaurus.minecraft.chameleon.util.WorldUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ItemFramedTank extends ItemTank
{
    public ItemFramedTank (Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock (@NotNull BlockPlaceContext context, @NotNull BlockState state) {
        if (!super.placeBlock(context, state))
            return false;

        BlockEntityFramedTank blockEntity = WorldUtils.getBlockEntity(context.getLevel(), context.getClickedPos(), BlockEntityFramedTank.class);
        if (blockEntity != null) {
            blockEntity.material().read(context.getItemInHand());
            blockEntity.setChanged();
        }

        return true;
    }
}
