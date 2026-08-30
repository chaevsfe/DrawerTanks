package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.BlockEntityDataShim;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityLinkedTank extends BlockEntityTank
{
    // default 1 bucket per second, moved only while both ends' chunks are loaded
    private static long transferPerTick () {
        return com.chaevsfe.drawertanks.config.TankConfig.linkedTransferMbPerTick * DROPLETS_PER_MB;
    }

    private GlobalPos partner;

    public BlockEntityLinkedTank (BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINKED_TANK.get(), pos, state);
        injectData(new LinkData());
    }

    public GlobalPos getPartner () {
        return partner;
    }

    public void setPartner (GlobalPos partner) {
        this.partner = partner;
        onContentsChanged();
    }

    public static void serverTickLinked (Level level, BlockPos pos, BlockState state, BlockEntityLinkedTank tank) {
        tank.pullFromPartner();
        BlockEntityTank.serverTick(level, pos, state, tank);
    }

    private void pullFromPartner () {
        if (partner == null || !(getLevel() instanceof ServerLevel serverLevel))
            return;

        ServerLevel sourceLevel = serverLevel.getServer().getLevel(partner.dimension());
        if (sourceLevel == null || !sourceLevel.isLoaded(partner.pos()))
            return;

        if (!(sourceLevel.getBlockEntity(partner.pos()) instanceof BlockEntityLinkedTank source)) {
            setPartner(null);
            return;
        }

        if (source == this) {
            setPartner(null);
            return;
        }

        // refuse to pull from a partner that pulls from us; a mutual link would slosh every tick
        GlobalPos self = GlobalPos.of(serverLevel.dimension(), getBlockPos());
        if (self.equals(source.getPartner()))
            return;

        TankData from = source.tankData();
        TankData to = tankData();
        if (from.isEmpty())
            return;
        if (!to.isEmpty() && !to.matches(from.getFluid(), from.getComponents()))
            return;

        long space = Math.max(0, capacityDroplets() - to.getAmount());
        long moved = Math.min(transferPerTick(), Math.min(from.getAmount(), space));
        if (moved <= 0)
            return;

        to.setFluid(from.getFluid(), from.getComponents());
        to.setAmount(to.getAmount() + moved);
        onContentsChanged();

        if (!source.isUnlimitedVending()) {
            from.setAmount(from.getAmount() - moved);
            source.onContentsChanged();
        }
    }

    @Override
    public void removeComponentsFromTag (ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Partner");
    }

    private class LinkData extends BlockEntityDataShim
    {
        @Override
        public void read (ValueInput input) {
            partner = input.read("Partner", GlobalPos.CODEC).orElse(null);
        }

        @Override
        public void write (ValueOutput output) {
            if (partner != null)
                output.store("Partner", GlobalPos.CODEC, partner);
            else
                output.discard("Partner");
        }
    }
}
