package com.chaevsfe.drawertanks.block;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedTank;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockLinkedTank extends BlockTank
{
    public BlockLinkedTank (Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity (BlockPos pos, BlockState state) {
        return new BlockEntityLinkedTank(pos, state);
    }

    @Override
    public net.minecraft.world.item.ItemStack makeFramedItem (net.minecraft.world.item.ItemStack source, net.minecraft.world.item.ItemStack matSide, net.minecraft.world.item.ItemStack matTrim, net.minecraft.world.item.ItemStack matFront) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker (Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.LINKED_TANK.get())
            return null;

        return (lvl, pos, st, be) -> BlockEntityLinkedTank.serverTickLinked(lvl, pos, st, (BlockEntityLinkedTank) be);
    }
}
