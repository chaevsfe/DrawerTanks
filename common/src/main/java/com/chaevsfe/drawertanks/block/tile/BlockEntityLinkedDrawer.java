package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.config.TankConfig;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.jaquadro.minecraft.storagedrawers.block.tile.BaseBlockEntity;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.BlockEntityDataShim;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockEntityLinkedDrawer extends BaseBlockEntity
{
    public static final int STRIPS = BlockEntityLinkedTank.STRIPS;

    private final DyeColor[] channels = new DyeColor[STRIPS];
    private long lastSeenVersion = Long.MIN_VALUE;
    private boolean syncPending;
    private long lastSyncTime = -100;
    private long lastTakeTime = Long.MIN_VALUE / 2;

    private ItemStack mirrorItem = ItemStack.EMPTY;
    private long mirrorCount;

    public BlockEntityLinkedDrawer (BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINKED_DRAWER.get(), pos, state);
        Arrays.fill(channels, DyeColor.WHITE);
        injectData(new DrawerData());
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
        onPoolChanged();
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
        onPoolChanged();
        return true;
    }

    // pooled items stay in the channel; only the channel itself travels with the item
    @Override
    protected void collectImplicitComponents (net.minecraft.core.component.DataComponentMap.Builder builder) {
        LinkedChannelData.collect(builder, channels);
    }

    @Override
    protected void applyImplicitComponents (net.minecraft.core.component.DataComponentGetter input) {
        LinkedChannelData.apply(input, channels);
    }

    public LinkedItemChannels.Pool pool () {
        if (!(getLevel() instanceof ServerLevel serverLevel))
            return null;
        return LinkedItemChannels.get(serverLevel.getServer()).pool(channelKey());
    }

    public ItemStack displayItem () {
        LinkedItemChannels.Pool pool = pool();
        return pool != null ? pool.prototype : mirrorItem;
    }

    public long displayCount () {
        LinkedItemChannels.Pool pool = pool();
        return pool != null ? pool.count : mirrorCount;
    }

    public long capacityItems () {
        return capacityItems(ItemStack.EMPTY);
    }

    // an empty channel has no prototype to size from, so fall back to what is being offered
    public long capacityItems (ItemStack forItem) {
        ItemStack reference = displayItem();
        if (reference.isEmpty())
            reference = forItem;
        return LinkedItemChannels.Pool.capacityFor(reference);
    }

    public boolean tryTake (long gameTime) {
        if (gameTime - lastTakeTime < 5)
            return false;

        lastTakeTime = gameTime;
        return true;
    }

    public float fillFraction () {
        long capacity = capacityItems();
        return capacity <= 0 ? 0 : Math.min(1f, (float) ((double) displayCount() / capacity));
    }

    public void onPoolChanged () {
        setChanged();
        if (!(getLevel() instanceof ServerLevel serverLevel))
            return;

        LinkedItemChannels store = LinkedItemChannels.get(serverLevel.getServer());
        store.setDirty();
        LinkedItemChannels.Pool pool = store.pool(channelKey());
        pool.version++;
        lastSeenVersion = pool.version;
        mirrorItem = pool.prototype.copy();
        mirrorCount = pool.count;
        requestSync();
    }

    private void requestSync () {
        long now = getLevel().getGameTime();
        if (now - lastSyncTime >= 4) {
            lastSyncTime = now;
            syncPending = false;
            markBlockForUpdate();
        } else
            syncPending = true;
    }

    public static void serverTick (Level level, BlockPos pos, BlockState state, BlockEntityLinkedDrawer drawer) {
        LinkedItemChannels.Pool pool = drawer.pool();
        if (pool != null && pool.version != drawer.lastSeenVersion) {
            drawer.lastSeenVersion = pool.version;
            drawer.mirrorItem = pool.prototype.copy();
            drawer.mirrorCount = pool.count;
            drawer.setChanged();
            drawer.requestSync();
        }

        if (drawer.syncPending && level.getGameTime() - drawer.lastSyncTime >= 4) {
            drawer.syncPending = false;
            drawer.lastSyncTime = level.getGameTime();
            drawer.markBlockForUpdate();
        }
    }

    private class DrawerData extends BlockEntityDataShim
    {
        @Override
        public void read (ValueInput input) {
            Arrays.fill(channels, DyeColor.WHITE);
            input.read("Channels", Codec.INT.listOf()).ifPresent(list -> {
                for (int i = 0; i < Math.min(list.size(), STRIPS); i++) {
                    DyeColor color = list.get(i) < 0 ? null : DyeColor.byId(list.get(i));
                    channels[i] = color == null ? DyeColor.WHITE : color;
                }
            });
            mirrorItem = input.read("MirrorItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            mirrorCount = input.read("MirrorCount", Codec.LONG).orElse(0L);
        }

        @Override
        public void write (ValueOutput output) {
            List<Integer> ids = new ArrayList<>();
            for (DyeColor color : channels)
                ids.add(color.getId());
            output.store("Channels", Codec.INT.listOf(), ids);
            if (!mirrorItem.isEmpty()) {
                output.store("MirrorItem", ItemStack.CODEC, mirrorItem);
                output.store("MirrorCount", Codec.LONG, mirrorCount);
            } else {
                output.discard("MirrorItem");
                output.discard("MirrorCount");
            }
        }
    }
}
