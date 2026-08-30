package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.jaquadro.minecraft.storagedrawers.api.framing.IFramedBlockEntity;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.FramedModelProperties;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.RenderDataProvider;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.MaterialData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockEntityFramedTank extends BlockEntityTank implements IFramedBlockEntity, RenderDataProvider
{
    private final MaterialData materialData = new MaterialData();

    public BlockEntityFramedTank (BlockPos pos, BlockState state) {
        this(ModBlockEntities.FRAMED_TANK.get(), pos, state);
    }

    protected BlockEntityFramedTank (BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        injectPortableData(materialData);
    }

    @Override
    public MaterialData material () {
        return materialData;
    }

    @Override
    public boolean dataPacketRequiresRenderUpdate () {
        return true;
    }

    @Override
    @Nullable
    public Object getRenderData () {
        return FramedModelProperties.getModelData(this);
    }
}
