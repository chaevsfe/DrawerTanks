package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.config.TankConfig;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.BlockEntityDataShim;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class BlockEntityLinkedTank extends BlockEntityTank
{
    public static final int STRIPS = 5;

    private final DyeColor[] channels = new DyeColor[STRIPS];
    private long lastSeenVersion = Long.MIN_VALUE;
    private boolean legacyContentsPending;

    public BlockEntityLinkedTank (BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINKED_TANK.get(), pos, state);
        java.util.Arrays.fill(channels, DyeColor.WHITE);
        injectData(new LinkData());
    }

    public DyeColor[] getChannels () {
        return channels;
    }

    public String channelKey () {
        StringBuilder key = new StringBuilder();
        for (DyeColor color : channels) {
            if (key.length() > 0)
                key.append(',');
            key.append(color.getId());
        }
        return key.toString();
    }

    public boolean setChannelDye (int strip, DyeColor color) {
        if (strip < 0 || strip >= STRIPS || channels[strip] == color)
            return false;

        channels[strip] = color;
        lastSeenVersion = Long.MIN_VALUE;
        Bridges.INVALIDATE_CAPS.accept(this);
        onContentsChanged();
        return true;
    }

    public boolean clearChannels () {
        boolean any = false;
        for (int i = 0; i < STRIPS; i++) {
            any |= channels[i] != DyeColor.WHITE;
            channels[i] = DyeColor.WHITE;
        }
        if (!any)
            return false;

        lastSeenVersion = Long.MIN_VALUE;
        Bridges.INVALIDATE_CAPS.accept(this);
        onContentsChanged();
        return true;
    }

    public LinkedChannels.Pool pool () {
        if (!(getLevel() instanceof ServerLevel serverLevel))
            return null;
        return LinkedChannels.get(serverLevel.getServer()).pool(channelKey());
    }

    @Override
    public TankData tankData () {
        LinkedChannels.Pool pool = pool();
        return pool != null ? pool.data : super.tankData();
    }

    public TankData clientMirror () {
        return super.tankData();
    }

    @Override
    public TankTarget target () {
        LinkedChannels.Pool pool = pool();
        return pool != null ? pool.target() : super.target();
    }

    @Override
    public long capacityDroplets () {
        return (long) TankConfig.linkedChannelCapacityBuckets * DROPLETS_PER_BUCKET;
    }

    @Override
    public boolean acceptsUpgrades () {
        return false;
    }

    @Override
    public void onContentsChanged () {
        if (getLevel() instanceof ServerLevel serverLevel) {
            LinkedChannels store = LinkedChannels.get(serverLevel.getServer());
            store.setDirty();
            store.pool(channelKey()).version++;
        }
        super.onContentsChanged();
    }

    public static void serverTickLinked (Level level, BlockPos pos, BlockState state, BlockEntityLinkedTank tank) {
        LinkedChannels.Pool pool = tank.pool();
        if (pool != null) {
            if (tank.legacyContentsPending) {
                tank.legacyContentsPending = false;
                TankData mirror = tank.clientMirror();
                if (!mirror.isEmpty() && (pool.data.isEmpty() || pool.data.matches(mirror.getFluid(), mirror.getComponents()))) {
                    long space = Math.max(0, tank.capacityDroplets() - pool.data.getAmount());
                    long moved = Math.min(space, mirror.getAmount());
                    if (moved > 0) {
                        pool.data.setFluid(mirror.getFluid(), mirror.getComponents());
                        pool.data.setAmount(pool.data.getAmount() + moved);
                        tank.onContentsChanged();
                    }
                }
                mirror.clear();
            }

            if (pool.version != tank.lastSeenVersion) {
                TankData mirror = tank.clientMirror();
                mirror.setFluid(pool.data.getFluid(), pool.data.getComponents());
                mirror.setAmount(pool.data.getAmount());
                tank.superOnContentsChanged();
                tank.lastSeenVersion = pool.version;
            }
        }

        BlockEntityTank.serverTick(level, pos, state, tank);
    }

    private void superOnContentsChanged () {
        super.onContentsChanged();
    }

    // channel contents live in the shared pool, not in the block or its dropped item
    @Override
    protected void collectImplicitComponents (net.minecraft.core.component.DataComponentMap.Builder builder) {
    }

    @Override
    protected void applyImplicitComponents (net.minecraft.core.component.DataComponentGetter input) {
    }

    private class LinkData extends BlockEntityDataShim
    {
        @Override
        public void read (ValueInput input) {
            java.util.Arrays.fill(channels, DyeColor.WHITE);
            input.read("Channels", Codec.INT.listOf()).ifPresent(list -> {
                for (int i = 0; i < Math.min(list.size(), STRIPS); i++) {
                    DyeColor color = list.get(i) < 0 ? null : DyeColor.byId(list.get(i));
                    channels[i] = color == null ? DyeColor.WHITE : color;
                }
            });

            // worlds from the coupler era stored fluid locally; fold it into the channel pool once
            legacyContentsPending = !input.read("Mirror", Codec.BOOL).orElse(false);
        }

        @Override
        public void write (ValueOutput output) {
            List<Integer> ids = new ArrayList<>();
            for (DyeColor color : channels)
                ids.add(color.getId());
            output.store("Channels", Codec.INT.listOf(), ids);
            output.store("Mirror", Codec.BOOL, true);
            output.discard("Partner");
        }
    }
}
