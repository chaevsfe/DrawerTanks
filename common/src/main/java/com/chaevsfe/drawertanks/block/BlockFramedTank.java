package com.chaevsfe.drawertanks.block;

import com.chaevsfe.drawertanks.block.tile.BlockEntityFramedTank;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.jaquadro.minecraft.storagedrawers.api.framing.FrameMaterial;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlock;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlockEntity;
import com.jaquadro.minecraft.storagedrawers.components.item.FrameData;
import com.texelsaurus.minecraft.chameleon.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlockFramedTank extends BlockTank implements IFramedBlock
{
    public BlockFramedTank (Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity (BlockPos pos, BlockState state) {
        return Bridges.FRAMED_TANK_FACTORY.create(pos, state);
    }

    @Override
    public void setPlacedBy (Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);

        BlockEntityFramedTank blockEntity = WorldUtils.getBlockEntity(level, pos, BlockEntityFramedTank.class);
        if (blockEntity != null) {
            blockEntity.material().read(stack);
            blockEntity.setChanged();
        }
    }

    @Override
    protected List<ItemStack> getDrops (BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        drops.add(getMainDrop(state, blockEntity instanceof BlockEntityFramedTank tank ? tank : null));
        return drops;
    }

    private ItemStack getMainDrop (BlockState state, @Nullable BlockEntityFramedTank tank) {
        ItemStack drop = new ItemStack(this);
        if (tank == null)
            return drop;

        if (!tank.material().isEmpty())
            drop.set(com.jaquadro.minecraft.storagedrawers.core.ModDataComponents.FRAME_DATA.get(), new FrameData(tank.material()));

        if (!tank.tankData().isEmpty())
            drop.set(ModDataComponents.TANK_CONTENTS.get(), tank.tankData().toContents());

        List<ItemStackWithSlot> upgrades = new ArrayList<>();
        for (int i = 0; i < tank.upgrades().getSlotCount(); i++) {
            ItemStack upgrade = tank.upgrades().getUpgrade(i);
            if (!upgrade.isEmpty())
                upgrades.add(new ItemStackWithSlot(i, upgrade));
        }
        if (!upgrades.isEmpty())
            drop.set(ModDataComponents.TANK_UPGRADES.get(), new com.chaevsfe.drawertanks.components.TankUpgrades(upgrades));

        return drop;
    }

    @Override
    public ItemStack getCloneItemStack (LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);

        BlockEntityFramedTank blockEntity = WorldUtils.getBlockEntity(level, pos, BlockEntityFramedTank.class);
        if (blockEntity != null && !blockEntity.material().isEmpty())
            stack.set(com.jaquadro.minecraft.storagedrawers.core.ModDataComponents.FRAME_DATA.get(), new FrameData(blockEntity.material()));

        return stack;
    }

    @Override
    public IFramedBlockEntity getFramedBlockEntity (Level level, BlockPos pos) {
        return WorldUtils.getBlockEntity(level, pos, BlockEntityFramedTank.class);
    }

    @Override
    public boolean supportsFrameMaterial (FrameMaterial material) {
        return material == FrameMaterial.SIDE;
    }

    @Override
    protected float getShadeBrightness (BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BlockEntityFramedTank tank && tank.material().allMatOpaque())
            return 0.8f;
        return 1f;
    }

    @Override
    protected boolean propagatesSkylightDown (BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getVisualShape (BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
