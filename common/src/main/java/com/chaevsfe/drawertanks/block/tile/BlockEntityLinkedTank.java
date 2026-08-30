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
    // the channel's capacity depends on its upgrades, which the client cannot resolve on its own
    private long mirrorCapacity = -1;

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
    protected com.jaquadro.minecraft.storagedrawers.capabilities.BasicDrawerAttributes attributes () {
        LinkedChannels.Pool pool = pool();
        return pool != null ? pool.attributes : super.attributes();
    }

    @Override
    public com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData upgrades () {
        LinkedChannels.Pool pool = pool();
        return pool != null ? pool.upgrades : super.upgrades();
    }

    @Override
    public long capacityDroplets () {
        LinkedChannels.Pool pool = pool();
        if (pool != null)
            return pool.capacityDroplets();

        return mirrorCapacity > 0 ? mirrorCapacity
            : (long) TankConfig.linkedChannelCapacityBuckets * DROPLETS_PER_BUCKET;
    }

    @Override
    public void onContentsChanged () {
        if (getLevel() instanceof ServerLevel serverLevel) {
            LinkedChannels store = LinkedChannels.get(serverLevel.getServer());
            store.setDirty();
            LinkedChannels.Pool pool = store.pool(channelKey());
            pool.version++;
            lastSeenVersion = pool.version;
            if (!legacyContentsPending) {
                TankData mirror = clientMirror();
                mirror.setFluid(pool.data.getFluid(), pool.data.getComponents());
                mirror.setAmount(pool.data.getAmount());
            }
        }
        super.onContentsChanged();
    }

    public static void serverTickLinked (Level level, BlockPos pos, BlockState state, BlockEntityLinkedTank tank) {
        LinkedChannels.Pool pool = tank.pool();
        if (pool != null) {
            if (tank.legacyContentsPending) {
                TankData mirror = tank.clientMirror();
                long moved = 0;
                if (!mirror.isEmpty() && (pool.data.isEmpty() || pool.data.matches(mirror.getFluid(), mirror.getComponents()))) {
                    long space = Math.max(0, tank.capacityDroplets() - pool.data.getAmount());
                    moved = Math.min(space, mirror.getAmount());
                    if (moved > 0) {
                        pool.data.setFluid(mirror.getFluid(), mirror.getComponents());
                        pool.data.setAmount(pool.data.getAmount() + moved);
                        mirror.setAmount(mirror.getAmount() - moved);
                    }
                }

                // whatever would not fit stays put and retries once the pool drains or matches.
                // this must settle before onContentsChanged, which refreshes the mirror from the
                // pool once the migration is done and would otherwise re-arm the fold.
                tank.legacyContentsPending = !mirror.isEmpty();
                if (moved > 0)
                    tank.onContentsChanged();
            }

            if (!tank.legacyContentsPending && pool.version != tank.lastSeenVersion) {
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

    // channel contents live in the shared pool; only the channel itself travels with the item
    @Override
    protected void collectImplicitComponents (net.minecraft.core.component.DataComponentMap.Builder builder) {
        LinkedChannelData.collect(builder, channels);
    }

    @Override
    protected void applyImplicitComponents (net.minecraft.core.component.DataComponentGetter input) {
        LinkedChannelData.apply(input, channels);

        // read both so they are consumed rather than stranded as ghost components on the item;
        // any contents ride the existing fold into the channel pool
        input.get(com.chaevsfe.drawertanks.core.ModDataComponents.TANK_UPGRADES.get());
        com.chaevsfe.drawertanks.components.TankContents contents =
            input.get(com.chaevsfe.drawertanks.core.ModDataComponents.TANK_CONTENTS.get());
        if (contents != null && !contents.isEmpty()) {
            clientMirror().fromContents(contents);
            legacyContentsPending = true;
        }
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

            mirrorCapacity = input.read("Capacity", Codec.LONG).orElse(-1L);

            // worlds from the coupler era stored fluid locally; fold it into the channel pool once
            legacyContentsPending = !input.read("Mirror", Codec.BOOL).orElse(false);
        }

        @Override
        public void write (ValueOutput output) {
            List<Integer> ids = new ArrayList<>();
            for (DyeColor color : channels)
                ids.add(color.getId());
            output.store("Channels", Codec.INT.listOf(), ids);
            output.store("Capacity", Codec.LONG, capacityDroplets());
            // only claim the fold has happened once it actually has, or a non-ticking chunk loses the fluid
            if (legacyContentsPending)
                output.discard("Mirror");
            else
                output.store("Mirror", Codec.BOOL, true);
            output.discard("Partner");
        }
    }
}
