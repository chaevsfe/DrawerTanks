package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.config.TankConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import com.jaquadro.minecraft.storagedrawers.capabilities.BasicDrawerAttributes;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkedItemChannels extends SavedData
{
    public static class Pool
    {
        public ItemStack prototype = ItemStack.EMPTY;
        public long count;
        public long version;

        LinkedItemChannels owner;
        // one handler per pool, so same-channel drawers share a single transaction participant
        public Object platformHandler;

        // the channel owns its upgrades, like the fluid channels do
        public final PoolAttributes attributes = new PoolAttributes();
        public final PoolUpgrades upgrades = new PoolUpgrades();

        public Pool () {
            upgrades.setDrawerAttributes(attributes);
        }

        public class PoolAttributes extends BasicDrawerAttributes
        {
            @Override
            protected void onAttributeChanged () {
                retainItem = isItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY);
                if (!retainItem && count <= 0)
                    prototype = ItemStack.EMPTY;
                changed();
            }
        }

        public class PoolUpgrades extends UpgradeData
        {
            public PoolUpgrades () {
                super(BlockEntityTank.UPGRADE_SLOTS);
            }

            @Override
            protected void onUpgradeChanged (ItemStack oldUpgrade, ItemStack newUpgrade) {
                changed();
            }

            List<ItemStackWithSlot> toList () {
                List<ItemStackWithSlot> out = new ArrayList<>();
                for (int i = 0; i < upgrades.length; i++) {
                    if (!upgrades[i].isEmpty())
                        out.add(new ItemStackWithSlot(i, upgrades[i].copy()));
                }
                return out;
            }

            void load (List<ItemStackWithSlot> stacks) {
                java.util.Arrays.fill(upgrades, ItemStack.EMPTY);
                for (ItemStackWithSlot slotStack : stacks) {
                    if (slotStack.isValidInContainer(upgrades.length))
                        upgrades[slotStack.slot()] = slotStack.stack().copy();
                }
                setDrawerAttributes(attributes);
            }
        }

        // a locked channel keeps its item type at zero so nothing else can be pumped in
        public boolean retainItem;

        // the raw saved entry when it could not be decoded; see ParkedChannel
        Dynamic<?> parked;

        public boolean isUnresolved () {
            return parked != null;
        }

        public boolean isEmpty () {
            return prototype.isEmpty() || count <= 0;
        }

        public boolean hasItem () {
            return !prototype.isEmpty();
        }

        public boolean accepts (ItemStack stack) {
            if (parked != null)
                return false;
            return !hasItem() || ItemStack.isSameItemSameComponents(prototype, stack);
        }

        // worth persisting if it holds items or upgrades
        public boolean isBlank () {
            if (parked != null || hasItem())
                return false;
            if (attributes.isItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY)
                || attributes.isConcealed() || attributes.isShowingQuantity())
                return false;

            for (int i = 0; i < upgrades.getSlotCount(); i++) {
                if (!upgrades.getUpgrade(i).isEmpty())
                    return false;
            }
            return true;
        }

        public void changed () {
            version++;
            if (owner != null)
                owner.setDirty();
        }

        public long capacity () {
            return capacityFor(prototype);
        }

        public long capacityFor (ItemStack reference) {
            if (attributes.isUnlimitedStorage() || attributes.isUnlimitedVending())
                return Long.MAX_VALUE / 4;

            int stackSize = reference.isEmpty() ? 64 : reference.getMaxStackSize();
            long stacks = (long) TankConfig.linkedChannelCapacityStacks * upgrades.getStorageMultiplier();
            if (upgrades.hasOneStackUpgrade())
                stacks = 1;

            return stacks * stackSize;
        }

        public void set (ItemStack prototype, long count) {
            this.prototype = prototype.isEmpty() ? ItemStack.EMPTY : prototype.copyWithCount(1);
            this.count = Math.max(0, count);
            if (this.count <= 0 && !retainItem)
                this.prototype = ItemStack.EMPTY;
        }
    }

    private record PoolContents(ItemStack prototype, long count, List<ItemStackWithSlot> upgrades,
                               boolean locked, boolean concealed, boolean showQuantity)
    {
        static final Codec<PoolContents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ItemStack.CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(PoolContents::prototype),
                Codec.LONG.optionalFieldOf("count", 0L).forGetter(PoolContents::count),
                ItemStackWithSlot.CODEC.listOf().optionalFieldOf("upgrades", List.of()).forGetter(PoolContents::upgrades),
                Codec.BOOL.optionalFieldOf("locked", false).forGetter(PoolContents::locked),
                Codec.BOOL.optionalFieldOf("concealed", false).forGetter(PoolContents::concealed),
                Codec.BOOL.optionalFieldOf("show_quantity", false).forGetter(PoolContents::showQuantity)
            ).apply(instance, PoolContents::new));
    }

    // entries are decoded one by one so a single unreadable channel is parked instead of taking
    // the rest of the file down with it or being dropped on the next save
    private static final Codec<LinkedItemChannels> CODEC = Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
        .xmap(LinkedItemChannels::fromRaw, LinkedItemChannels::toRaw);

    // both loaders currently patch vanilla's unguarded DataFixTypes.update call to tolerate null,
    // but vanilla itself does not; pass a real constant rather than depend on that patch
    public static final SavedDataType<LinkedItemChannels> TYPE = new SavedDataType<>(
        ModConstants.loc("linked_item_channels"), LinkedItemChannels::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final Map<String, Pool> pools = new HashMap<>();

    public static LinkedItemChannels get (MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Pool pool (String key) {
        return pools.computeIfAbsent(key, k -> {
            Pool pool = new Pool();
            pool.owner = this;
            return pool;
        });
    }

    private static LinkedItemChannels fromRaw (Map<String, Dynamic<?>> map) {
        LinkedItemChannels channels = new LinkedItemChannels();
        map.forEach((key, raw) -> {
            Pool pool = channels.pool(key);
            DataResult<PoolContents> parsed = PoolContents.CODEC.parse(raw);
            if (parsed.result().isEmpty()) {
                pool.parked = raw;
                ParkedChannel.report("item", key + " " + ParkedChannel.describe(raw), parsed.error().map(Object::toString).orElse("?"));
                return;
            }
            PoolContents contents = parsed.result().get();
            pool.upgrades.load(contents.upgrades());
            pool.attributes.setItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY, contents.locked());
            pool.attributes.setItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_POPULATED, contents.locked());
            pool.attributes.setIsConcealed(contents.concealed());
            pool.attributes.setIsShowingQuantity(contents.showQuantity());
            pool.retainItem = contents.locked();
            pool.set(contents.prototype(), contents.count());
        });
        return channels;
    }

    private Map<String, Dynamic<?>> toRaw () {
        Map<String, Dynamic<?>> out = new HashMap<>();
        pools.forEach((key, pool) -> {
            if (pool.parked != null) {
                out.put(key, pool.parked);
                return;
            }
            if (pool.isBlank())
                return;

            PoolContents contents = new PoolContents(pool.prototype, pool.count, pool.upgrades.toList(),
                pool.attributes.isItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY),
                pool.attributes.isConcealed(),
                pool.attributes.isShowingQuantity());
            PoolContents.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, contents).result()
                .ifPresent(tag -> out.put(key, new Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, tag)));
        });
        return out;
    }
}
