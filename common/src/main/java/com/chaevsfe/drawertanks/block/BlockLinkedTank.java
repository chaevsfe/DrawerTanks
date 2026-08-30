package com.chaevsfe.drawertanks.block;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedTank;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (level.getBlockEntity(pos) instanceof BlockEntityLinkedTank tank && tank.addChannelDye(dye)) {
                if (!player.hasInfiniteMaterials())
                    stack.shrink(1);
                player.sendOverlayMessage(Component.translatable("message.drawertanks.linked.dyed", tank.getChannels().size(), BlockEntityLinkedTank.MAX_DYES));
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        if (stack.is(Items.SPONGE) || stack.is(Items.WET_SPONGE)) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (level.getBlockEntity(pos) instanceof BlockEntityLinkedTank tank && tank.clearChannels()) {
                player.sendOverlayMessage(Component.translatable("message.drawertanks.linked.cleared"));
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit);
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
