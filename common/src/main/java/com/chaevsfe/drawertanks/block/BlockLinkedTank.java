package com.chaevsfe.drawertanks.block;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedTank;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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
    public ItemStack makeFramedItem (ItemStack source, ItemStack matSide, ItemStack matTrim, ItemStack matFront) {
        return ItemStack.EMPTY;
    }

    @Override
    protected InteractionResult useItemOn (ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        DyeColor dye = dyeFrom(stack);
        if (dye != null) {
            if (hit.getDirection() != Direction.UP)
                return InteractionResult.PASS;

            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (level.getBlockEntity(pos) instanceof BlockEntityLinkedTank tank) {
                int strip = stripAt(state.getValue(FACING), hit, pos);
                if (tank.setChannelDye(strip, dye)) {
                    if (!player.hasInfiniteMaterials())
                        stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        if (stack.is(Items.SPONGE) || stack.is(Items.WET_SPONGE)) {
            // clearing a channel is a deliberate lid gesture, like dyeing it
            if (hit.getDirection() != Direction.UP)
                return InteractionResult.PASS;

            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (level.getBlockEntity(pos) instanceof BlockEntityLinkedTank tank && tank.clearChannels()) {
                player.sendOverlayMessage(Component.translatable("message.drawertanks.linked.cleared"));
                level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, .5f, 1f);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    // map the clicked point on the lid into the renderer's aligned frame, so the dyed strip
    // is the one under the cursor for every facing
    private static int stripAt (Direction facing, BlockHitResult hit, BlockPos pos) {
        double wx = hit.getLocation().x - pos.getX();
        double wz = hit.getLocation().z - pos.getZ();
        double across = switch (facing) {
            case NORTH -> 1 - wx;
            case EAST -> 1 - wz;
            case WEST -> wz;
            default -> wx;
        };
        int px = (int) Math.floor(across * 16);
        return Math.max(0, Math.min(BlockEntityLinkedTank.STRIPS - 1, (px - 1) / 3));
    }

    private static DyeColor dyeFrom (ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("minecraft") || !id.getPath().endsWith("_dye"))
            return null;

        for (DyeColor color : DyeColor.values()) {
            if (id.getPath().equals(color.getSerializedName() + "_dye"))
                return color;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker (Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.LINKED_TANK.get())
            return null;

        return (lvl, pos, st, be) -> BlockEntityLinkedTank.serverTickLinked(lvl, pos, st, (BlockEntityLinkedTank) be);
    }
}
