package com.chaevsfe.drawertanks.block;

import com.texelsaurus.minecraft.chameleon.inventory.ContentMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// vanilla skips the block entirely when a sneaking player holds anything in either hand, so an
// offhand item silently blocks shift-clicking into the menu; the loader hooks run this first
public final class OffhandMenuOpen
{
    private OffhandMenuOpen () { }

    public static InteractionResult tryOpen (Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || player.isSpectator())
            return InteractionResult.PASS;

        // vanilla already reaches the block for every other hand combination
        if (!player.getMainHandItem().isEmpty() || player.getOffhandItem().isEmpty())
            return InteractionResult.PASS;

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockTank) && !(state.getBlock() instanceof BlockLinkedDrawer))
            return InteractionResult.PASS;

        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        if (state.getMenuProvider(level, pos) instanceof ContentMenuProvider<?> provider && player instanceof ServerPlayer serverPlayer) {
            provider.openMenu(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
