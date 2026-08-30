package com.chaevsfe.drawertanks.block;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.jaquadro.minecraft.storagedrawers.core.ModItems;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgrade;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockTank extends HorizontalDirectionalBlock implements EntityBlock
{
    public static final MapCodec<BlockTank> CODEC = simpleCodec(BlockTank::new);

    public BlockTank (Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec () {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition (StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement (BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity (BlockPos pos, BlockState state) {
        return new BlockEntityTank(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker (Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.TANK.get())
            return null;

        return (lvl, pos, st, be) -> BlockEntityTank.serverTick(lvl, pos, st, (BlockEntityTank) be);
    }

    @Override
    protected InteractionResult useItemOn (ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ItemUpgrade) {
            if (!(level.getBlockEntity(pos) instanceof BlockEntityTank tank))
                return InteractionResult.PASS;

            if (stack.getItem() == ModItems.ONE_STACK_UPGRADE.get()
                && tank.tankData().getAmount() > BlockEntityTank.DROPLETS_PER_BUCKET)
                return InteractionResult.PASS;

            if (!tank.upgrades().canAddUpgrade(stack))
                return InteractionResult.PASS;

            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (tank.upgrades().addUpgrade(stack)) {
                if (!player.hasInfiniteMaterials())
                    stack.shrink(1);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        if (Bridges.FLUID != null && Bridges.FLUID.isFluidContainer(stack)) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            return Bridges.FLUID.interact(player, hand, level, pos, hit.getDirection())
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem (BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown())
            return InteractionResult.PASS;

        if (!(level.getBlockEntity(pos) instanceof BlockEntityTank tank))
            return InteractionResult.PASS;

        if (level.isClientSide())
            return tank.hasAnyUpgrade() ? InteractionResult.SUCCESS : InteractionResult.PASS;

        ItemStack removed = tank.tryRemoveUpgrade();
        if (removed.isEmpty())
            return InteractionResult.PASS;

        player.getInventory().placeItemBackInInventory(removed);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval (BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!level.getBlockState(pos).is(state.getBlock()))
            level.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    public boolean hasAnalogOutputSignal (@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal (BlockState state, Level level, BlockPos pos, @Nullable Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityTank tank))
            return 0;

        if (tank.tankData().isEmpty())
            return 0;

        return Math.min(15, 1 + (int) (tank.fillFraction() * 14));
    }
}
