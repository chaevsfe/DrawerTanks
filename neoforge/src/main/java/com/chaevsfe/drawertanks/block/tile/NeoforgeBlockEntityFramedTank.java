package com.chaevsfe.drawertanks.block.tile;

import com.jaquadro.minecraft.storagedrawers.client.model.NeoforgeModelData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

public class NeoforgeBlockEntityFramedTank extends BlockEntityFramedTank
{
    public NeoforgeBlockEntityFramedTank (BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public ModelData getModelData () {
        return NeoforgeModelData.of(this);
    }
}
