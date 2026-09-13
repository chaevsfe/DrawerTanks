package com.chaevsfe.drawertanks.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

public class NeoforgeBlockEntityFramedTank extends BlockEntityFramedTank
{
    // the unofficial port's model reads render data from ModelData under its NeoforgeModelData.RENDER_DATA
    // property; upstream's model reads the block entity itself and has no such class, so the property
    // is looked up by name and the empty data is enough there
    private static final ModelProperty<Object> PORT_RENDER_DATA = findPortProperty();

    public NeoforgeBlockEntityFramedTank (BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public ModelData getModelData () {
        Object data = PORT_RENDER_DATA == null ? null : getRenderData();
        return data == null ? ModelData.EMPTY : ModelData.of(PORT_RENDER_DATA, data);
    }

    @SuppressWarnings("unchecked")
    private static ModelProperty<Object> findPortProperty () {
        try {
            Class<?> holder = Class.forName("com.jaquadro.minecraft.storagedrawers.client.model.NeoforgeModelData");
            return (ModelProperty<Object>) holder.getField("RENDER_DATA").get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
