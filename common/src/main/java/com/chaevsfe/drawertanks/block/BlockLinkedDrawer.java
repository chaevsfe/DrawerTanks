package com.chaevsfe.drawertanks.block;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgrade;
import com.chaevsfe.drawertanks.block.tile.LinkedItemChannels;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import com.texelsaurus.minecraft.chameleon.inventory.ContentMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class BlockLinkedDrawer extends HorizontalDirectionalBlock implements EntityBlock
{
    public static final MapCodec<BlockLinkedDrawer> CODEC = simpleCodec(BlockLinkedDrawer::new);

    public BlockLinkedDrawer (Properties properties) {
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
        return new BlockEntityLinkedDrawer(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker (Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.LINKED_DRAWER.get())
            return null;

        return (lvl, pos, st, be) -> BlockEntityLinkedDrawer.serverTick(lvl, pos, st, (BlockEntityLinkedDrawer) be);
    }

    @Override
    protected InteractionResult useItemOn (ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        DyeColor dye = dyeFrom(stack);
        if (dye != null) {
            if (hit.getDirection() != Direction.UP)
                return InteractionResult.PASS;

            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (level.getBlockEntity(pos) instanceof BlockEntityLinkedDrawer drawer) {
                int strip = stripAt(state.getValue(FACING), hit, pos);
                if (drawer.setChannelDye(strip, dye) && !player.hasInfiniteMaterials())
                    stack.shrink(1);
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

            if (level.getBlockEntity(pos) instanceof BlockEntityLinkedDrawer drawer && drawer.clearChannels()) {
                player.sendOverlayMessage(Component.translatable("message.drawertanks.linked.cleared"));
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        // upgrades go into the channel rather than being stored as items
        if (stack.getItem() instanceof ItemUpgrade) {
            if (!(level.getBlockEntity(pos) instanceof BlockEntityLinkedDrawer drawer))
                return InteractionResult.PASS;

            if (!BlockEntityTank.upgradeApplies(stack) || !drawer.upgrades().canAddUpgrade(stack))
                return InteractionResult.PASS;

            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (drawer.upgrades().addUpgrade(stack)) {
                drawer.refreshUpgradeMirror();
                drawer.onPoolChanged();
                if (!player.hasInfiniteMaterials())
                    stack.shrink(1);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        // let Storage Drawers keys run their own useOn instead of being stored
        if (stack.getItem() instanceof com.jaquadro.minecraft.storagedrawers.item.ItemKey)
            return InteractionResult.PASS;

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem (BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // an empty hand opens the upgrade screen; holding something puts it in
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;

            if (state.getMenuProvider(level, pos) instanceof ContentMenuProvider<?> provider && player instanceof ServerPlayer serverPlayer) {
                provider.openMenu(serverPlayer);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        return putItems(level, pos, player);
    }

    @Override
    protected MenuProvider getMenuProvider (BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityLinkedDrawer drawer))
            return null;

        return new BlockEntityLinkedDrawer.ContentProvider(drawer);
    }

    // Right-click puts the held stack in; sneak is the take modifier, matching Storage Drawers.
    public InteractionResult putItems (Level level, BlockPos pos, Player player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty())
            return InteractionResult.PASS;

        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof BlockEntityLinkedDrawer drawer))
            return InteractionResult.PASS;

        LinkedItemChannels.Pool pool = drawer.pool();
        if (pool == null)
            return InteractionResult.PASS;

        if (!pool.accepts(stack))
            return InteractionResult.FAIL;

        int moved = insert(drawer, pool, stack);
        if (moved <= 0)
            return InteractionResult.FAIL;

        drawer.onPoolChanged();
        return InteractionResult.SUCCESS;
    }

    private static int insert (BlockEntityLinkedDrawer drawer, LinkedItemChannels.Pool pool, ItemStack stack) {
        if (stack.isEmpty())
            return 0;
        if (!pool.accepts(stack))
            return 0;

        long space = drawer.capacityItems(stack) - pool.count;
        int moved = (int) Math.min(space, stack.getCount());
        if (moved <= 0)
            return 0;

        if (!pool.hasItem())
            pool.set(stack, moved);
        else
            pool.count += moved;
        stack.shrink(moved);
        return moved;
    }

    public void takeItem (Level level, BlockPos pos, Player player, boolean single) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BlockEntityLinkedDrawer drawer))
            return;

        // the loader hooks fire before vanilla's own checks, so gate reach and build permission here
        if (player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.isWithinBlockInteractionRange(pos, 1.0)
                || !level.mayInteract(serverPlayer, pos)
                || serverPlayer.blockActionRestricted(level, pos, serverPlayer.gameMode.getGameModeForPlayer()))
                return;
        }

        if (!drawer.tryTake(level.getGameTime()))
            return;

        LinkedItemChannels.Pool pool = drawer.pool();
        if (pool == null || pool.isEmpty())
            return;

        int amount = single ? 1 : (int) Math.min(pool.prototype.getMaxStackSize(), pool.count);
        ItemStack taken = pool.prototype.copyWithCount(amount);
        pool.count -= amount;
        if (pool.count <= 0)
            pool.set(ItemStack.EMPTY, 0);
        drawer.onPoolChanged();
        player.getInventory().placeItemBackInInventory(taken);
    }

    @Override
    public boolean hasAnalogOutputSignal (@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal (BlockState state, Level level, BlockPos pos, @Nullable Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityLinkedDrawer drawer))
            return 0;

        if (drawer.displayCount() <= 0)
            return 0;

        return Math.min(15, 1 + (int) (drawer.fillFraction() * 14));
    }

    // shared with BlockLinkedTank's strip targeting
    static int stripAt (Direction facing, BlockHitResult hit, BlockPos pos) {
        double wx = hit.getLocation().x - pos.getX();
        double wz = hit.getLocation().z - pos.getZ();
        double across = switch (facing) {
            case NORTH -> 1 - wx;
            case EAST -> 1 - wz;
            case WEST -> wz;
            default -> wx;
        };
        int px = (int) Math.floor(across * 16);
        return Math.max(0, Math.min(BlockEntityLinkedDrawer.STRIPS - 1, (px - 1) / 3));
    }

    static DyeColor dyeFrom (ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("minecraft") || !id.getPath().endsWith("_dye"))
            return null;

        for (DyeColor color : DyeColor.values()) {
            if (id.getPath().equals(color.getSerializedName() + "_dye"))
                return color;
        }
        return null;
    }
}
