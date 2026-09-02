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

public class BlockEntityLinkedDrawer extends BaseBlockEntity implements com.chaevsfe.drawertanks.inventory.UpgradeHost
{
    public static final int STRIPS = BlockEntityLinkedTank.STRIPS;

    private final DyeColor[] channels = new DyeColor[STRIPS];
    private long lastSeenVersion = Long.MIN_VALUE;
    private boolean syncPending;
    private long lastSyncTime = -100;
    private long lastTakeTime = Long.MIN_VALUE / 2;
    private long mirrorCapacity = -1;

    private ItemStack mirrorItem = ItemStack.EMPTY;
    private long mirrorCount;

    // the channel owns the real upgrades; this copy exists so the client can see them
    private final MirrorAttributes mirrorAttributes = new MirrorAttributes();
    private final MirrorUpgrades mirrorUpgrades = new MirrorUpgrades();

    private class MirrorAttributes extends com.jaquadro.minecraft.storagedrawers.capabilities.BasicDrawerAttributes { }

    private class MirrorUpgrades extends com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData
    {
        MirrorUpgrades () {
            super(BlockEntityTank.UPGRADE_SLOTS);
        }

        void mirror (com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData source) {
            for (int i = 0; i < upgrades.length; i++)
                upgrades[i] = i < source.getSlotCount() ? source.getUpgrade(i).copy() : ItemStack.EMPTY;
            setDrawerAttributes(mirrorAttributes);
        }
    }

    public BlockEntityLinkedDrawer (BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINKED_DRAWER.get(), pos, state);
        Arrays.fill(channels, DyeColor.WHITE);
        mirrorUpgrades.setDrawerAttributes(mirrorAttributes);
        injectData(mirrorUpgrades);
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
        if (strip < 0 || strip >= STRIPS || channels[strip] == color || isChannelLocked())
            return false;

        channels[strip] = color;
        lastSeenVersion = Long.MIN_VALUE;
        Bridges.INVALIDATE_CAPS.accept(this);
        onPoolChanged();
        return true;
    }

    public boolean clearChannels () {
        if (isChannelLocked())
            return false;

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
        // setting null removes a snapshot the block entity may still be carrying from its item
        LinkedItemChannels.Pool pool = pool();
        builder.set(com.chaevsfe.drawertanks.core.ModDataComponents.LINK_ITEM.get(),
            pool != null && pool.hasItem()
                ? net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(pool.prototype.copyWithCount(1))
                : null);
    }

    @Override
    protected void applyImplicitComponents (net.minecraft.core.component.DataComponentGetter input) {
        LinkedChannelData.apply(input, channels);
        // read so vanilla drops it instead of keeping it on the block as a stale component
        input.get(com.chaevsfe.drawertanks.core.ModDataComponents.LINK_ITEM.get());
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

        LinkedItemChannels.Pool pool = pool();
        if (pool != null)
            return pool.capacityFor(reference);

        return mirrorCapacity > 0 ? mirrorCapacity
            : (long) TankConfig.linkedChannelCapacityStacks * (reference.isEmpty() ? 64 : reference.getMaxStackSize());
    }

    // Storage Drawers keys act through this capability
    public com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributesModifiable getDrawerAttributes () {
        LinkedItemChannels.Pool pool = pool();
        return pool != null ? pool.attributes : mirrorAttributes;
    }

    public boolean isConcealed () {
        return getDrawerAttributes().isConcealed();
    }

    public boolean isShowingQuantity () {
        return getDrawerAttributes().isShowingQuantity();
    }

    public boolean isChannelLocked () {
        return getDrawerAttributes().isItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY);
    }

    @Override
    public com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData upgrades () {
        LinkedItemChannels.Pool pool = pool();
        return pool != null ? pool.upgrades : mirrorUpgrades;
    }

    @Override
    public boolean acceptsUpgrades () {
        return true;
    }

    @Override
    public void refreshUpgradeMirror () {
        LinkedItemChannels.Pool pool = pool();
        if (pool != null)
            mirrorUpgrades.mirror(pool.upgrades);
    }

    @Override
    public boolean upgradeFitsContents (ItemStack upgrade) {
        return true;
    }

    @Override
    public long storedAmount () {
        return displayCount();
    }

    @Override
    public long capacityWithout (int slot) {
        return capacityWithSwap(slot, ItemStack.EMPTY);
    }

    @Override
    public long capacityWithSwap (int slot, ItemStack incoming) {
        com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData source = upgrades();
        java.util.List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < source.getSlotCount(); i++)
            list.add(i == slot ? incoming : source.getUpgrade(i));

        ItemStack reference = displayItem();
        int stackSize = reference.isEmpty() ? 64 : reference.getMaxStackSize();
        long stacks = BlockEntityTank.computeCapacityDroplets(list, TankConfig.linkedChannelCapacityStacks)
            / BlockEntityTank.DROPLETS_PER_BUCKET;
        return stacks * stackSize;
    }

    @Override
    public Level hostLevel () {
        return getLevel();
    }

    @Override
    public BlockPos hostPos () {
        return getBlockPos();
    }

    @Override
    public void hostChanged () {
        setChanged();
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
        mirrorUpgrades.mirror(pool.upgrades);
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
            drawer.mirrorUpgrades.mirror(pool.upgrades);
            drawer.setChanged();
            drawer.requestSync();
        }

        if (drawer.syncPending && level.getGameTime() - drawer.lastSyncTime >= 4) {
            drawer.syncPending = false;
            drawer.lastSyncTime = level.getGameTime();
            drawer.markBlockForUpdate();
        }
    }

    public net.minecraft.network.chat.Component getDisplayName () {
        return net.minecraft.network.chat.Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    public static class ContentProvider implements com.texelsaurus.minecraft.chameleon.inventory.ContentMenuProvider<com.texelsaurus.minecraft.chameleon.inventory.content.PositionContent>
    {
        private final BlockEntityLinkedDrawer entity;

        public ContentProvider (BlockEntityLinkedDrawer entity) {
            this.entity = entity;
        }

        @Override
        public com.texelsaurus.minecraft.chameleon.inventory.content.PositionContent createContent (net.minecraft.server.level.ServerPlayer player) {
            return new com.texelsaurus.minecraft.chameleon.inventory.content.PositionContent(entity.getBlockPos());
        }

        @Override
        public net.minecraft.network.chat.Component getDisplayName () {
            return entity.getDisplayName();
        }

        @Override
        public net.minecraft.world.inventory.AbstractContainerMenu createMenu (int id, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.world.entity.player.Player player) {
            return new com.chaevsfe.drawertanks.inventory.ContainerTank(id, inventory, entity);
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
            mirrorCapacity = input.read("Capacity", Codec.LONG).orElse(-1L);
            boolean locked = input.read("Locked", Codec.BOOL).orElse(false);
            mirrorAttributes.setItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY, locked);
            mirrorAttributes.setItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_POPULATED, locked);
            mirrorAttributes.setIsConcealed(input.read("Concealed", Codec.BOOL).orElse(false));
            mirrorAttributes.setIsShowingQuantity(input.read("ShowQuantity", Codec.BOOL).orElse(false));
            mirrorItem = input.read("MirrorItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            mirrorCount = input.read("MirrorCount", Codec.LONG).orElse(0L);
        }

        @Override
        public void write (ValueOutput output) {
            List<Integer> ids = new ArrayList<>();
            for (DyeColor color : channels)
                ids.add(color.getId());
            output.store("Channels", Codec.INT.listOf(), ids);
            output.store("Capacity", Codec.LONG, capacityItems());
            output.store("Locked", Codec.BOOL, isChannelLocked());
            output.store("Concealed", Codec.BOOL, getDrawerAttributes().isConcealed());
            output.store("ShowQuantity", Codec.BOOL, getDrawerAttributes().isShowingQuantity());
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
